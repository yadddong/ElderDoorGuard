package com.syd.elderguard.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.content.FileProvider
import cn.bmob.v3.datatype.BmobFile
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.UpdateListener
import cn.bmob.v3.listener.UploadFileListener
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import com.syd.elderguard.BaseApplication
import com.syd.elderguard.R
import com.syd.elderguard.model.User
import com.syd.elderguard.ui.base.BaseActivity
import com.syd.elderguard.ui.dialog.ActionSheetDialog
import com.syd.elderguard.utils.CropUtils
import com.syd.elderguard.utils.FileUtil
import kotlinx.android.synthetic.main.activity_user_info.*
import java.io.File


class UserInfoActivity : BaseActivity(), View.OnClickListener {

    private val REQUEST_CODE_TAKE_PHOTO = 1
    private val REQUEST_CODE_ALBUM = 2
    private val REQUEST_CODE_CROUP_PHOTO = 3

    private lateinit var file: File
    private var uri: Uri? = null

    override fun getLayoutId(): Int {
        return R.layout.activity_user_info
    }

    override fun showToolbar(): Boolean {
        return true
    }

    override fun getToolbarTitleId(): Int {
        return R.string.title_user_info
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imvHeader.setOnClickListener(this)
        rlNickname.setOnClickListener(this)
        btnLogout.setOnClickListener(this)
        init()
    }

    override fun initImmersionBar() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .transparentNavigationBar()
            .navigationBarDarkIcon(true)
            .init();
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            when(p0.id) {
                R.id.imvHeader-> {
                    showHeaderDialog()
                }
                R.id.rlNickname-> {
                    showNicknameDialog()
                }
                R.id.btnLogout-> {
                    User.logOut()
                    finish()
                }

            }
        }
    }

    private fun showNicknameDialog() {
        MaterialDialog(this).show {
            title(R.string.dialog_title_modify_nickname)
            input(maxLength = 10, hintRes = R.string.hint_nickname) { _, text ->
                this@UserInfoActivity.txvNickname.text = text
                updateNickname(text.toString())
            }
            positiveButton(R.string.lable_ok)
        }
    }

    private fun updateNickname(nickName: String) {
        val user = User.getCurrentUser(User::class.java)
        user.nickName = nickName
        user.update(object : UpdateListener() {
            override fun done(e: BmobException) {
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != -1) {
            return
        }
        if (requestCode == REQUEST_CODE_ALBUM && data != null) {
            //val newUri: Uri?
            val newUri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Uri.parse("file:///" + CropUtils.getPath(this, data.data))
            } else {
                data.data
            }

            if (newUri != null) {
                startPhotoZoom(newUri)
            } else {
                showLongToast("????????????????????????")
            }
        } else if (requestCode == REQUEST_CODE_TAKE_PHOTO) {
            startPhotoZoom(uri)
        } else if (requestCode == REQUEST_CODE_CROUP_PHOTO) {
            uploadAvatarFromPhoto()
        }
    }

    private fun uploadAvatarFromPhoto() {
        val path = file.path
        val cover = FileUtil.getSmallBitmap(this, path)
        Glide.with(this).load(cover).into(imvHeader!!)
        val bmobFile = BmobFile(file)
        bmobFile.uploadblock(object : UploadFileListener() {
            override fun done(e: BmobException?) {
                if (e == null) {
                    val user = User.getCurrentUser(User::class.java)
                    user.avatar = bmobFile
                    user.update(object : UpdateListener() {
                        override fun done(e: BmobException?) {
                            if (e == null) {
                                showLongToast("????????????")
                            }
                        }
                    })
                } else {
                    showLongToast("??????????????????"+e.errorCode.toString())
                }
            }

            override fun onProgress(value: Int) {
                // ????????????????????????????????????
            }
        })
    }

    private fun showHeaderDialog() {
        ActionSheetDialog(this)
            .builder()
            .setCancelable(true)
            .setCanceledOnTouchOutside(true)
            .addSheetItem(
                "???????????????",
                ActionSheetDialog.SheetItemColor.Blue
            ) { uploadAvatarFromAlbumRequest() }
            .addSheetItem(
                "????????????",
                ActionSheetDialog.SheetItemColor.Blue
            ) { uploadAvatarFromPhotoRequest() }
            .show()
    }

    /**
     * photo
     */
    private fun uploadAvatarFromPhotoRequest() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO)
    }

    fun startPhotoZoom(uri: Uri?) {
        val intent = Intent("com.android.camera.action.CROP")
        intent.setDataAndType(uri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra("crop", "true") // crop=true ??????????????????????????????????????????.
        intent.putExtra("aspectX", 1) // ??????????????????????????????.
        intent.putExtra("aspectY", 1) // x:y=1:1
        //        intent.putExtra("outputX", 400);//??????????????????
//        intent.putExtra("outputY", 400);
        intent.putExtra("output", Uri.fromFile(file))
        intent.putExtra("outputFormat", "JPEG") // ????????????
        startActivityForResult(intent, REQUEST_CODE_CROUP_PHOTO)
    }

    /**
     * album
     */
    private fun uploadAvatarFromAlbumRequest() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, REQUEST_CODE_ALBUM)
    }

    private fun init() {
        file = File(FileUtil.getCachePath(this), "user-avatar.jpg")
        uri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Uri.fromFile(file)
        } else {
            //??????FileProvider????????????content?????????Uri(android 7.0????????????????????????????????????)
            FileProvider.getUriForFile(BaseApplication.inst,"$packageName.fileprovider", file)
        }
        val user: User = User.getCurrentUser(User::class.java)
        txvNickname?.text = user.nickName
        if (user.avatar != null) {
            val bmobFile: BmobFile = user.avatar
            Glide.with(this).load(bmobFile.fileUrl).into(imvHeader!!)
        }

    }
}