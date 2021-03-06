package com.hotbitmapgg.gank.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hotbitmapgg.gank.base.RxBaseActivity;
import com.hotbitmapgg.gank.utils.GlideDownloadImageUtil;
import com.hotbitmapgg.studyproject.R;
import com.hotbitmapgg.gank.config.ConstantUtil;
import com.hotbitmapgg.gank.utils.ImmersiveUtil;
import com.jakewharton.rxbinding.view.RxMenuItem;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import butterknife.Bind;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by hcc on 16/6/12.
 * <p/>
 * 福利大图浏览界面
 * Tips:系统NavBar和StatusBar状态栏隐藏方法:
 * <p/>
 * this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
 * mAppBarLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
 * <p/>
 * 针对6.0的权限问题进行处理
 * ToolBar单独使用的一些技巧,不与ActionBar进行一起使用
 * 使用mToolBar.inflateMenu(R.menu.xxx)即可
 */
public class FuliFullPicActivity extends RxBaseActivity
{


    @Bind(R.id.full_pic)
    ImageView mImageView;

    @Bind(R.id.app_bar_layout)
    AppBarLayout mAppBarLayout;

    @Bind(R.id.toolbar)
    Toolbar mToolBar;

    private static final String EXTRA_URL = "extra_url";

    private static final String EXTRA_TITLE = "extra_title";

    public static final String TRANSIT_PIC = "picture";

    private String url;

    private String title;

    private boolean isHide = false;

    @Override
    public int getLayoutId()
    {

        return R.layout.activity_full_pic;
    }

    @Override
    public void initViews(Bundle savedInstanceState)
    {

        Intent intent = getIntent();
        if (intent != null)
        {
            url = intent.getStringExtra(EXTRA_URL);
            title = intent.getStringExtra(EXTRA_TITLE);
        }

        ViewCompat.setTransitionName(mImageView, TRANSIT_PIC);

        Glide.with(this).load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(mImageView);

        setUpPhotoAttacher();
    }

    @Override
    public void initToolBar()
    {

        mToolBar.setTitle(title);
        mToolBar.setNavigationIcon(R.drawable.back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {

                onBackPressed();
            }
        });
        mToolBar.inflateMenu(R.menu.menu_meizi);

        mAppBarLayout.setAlpha(0.5f);
        mToolBar.setBackgroundResource(R.color.black_90);
        mAppBarLayout.setBackgroundResource(R.color.black_90);

        saveImage();
        shareImage();
    }

    public static Intent LuanchActivity(Activity activity, String url, String title)
    {

        Intent intent = new Intent(activity, FuliFullPicActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TITLE, title);
        return intent;
    }

    private void setUpPhotoAttacher()
    {

        mImageView.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {

                //隐藏ToolBar
                hideOrShowToolbar();
            }
        });

        mImageView.setOnLongClickListener(new View.OnLongClickListener()
        {

            @Override
            public boolean onLongClick(View v)
            {

                new AlertDialog.Builder(FuliFullPicActivity.this)
                        .setMessage("是否保存到本地?")
                        .setNegativeButton("取消", new DialogInterface.OnClickListener()
                        {

                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {

                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener()
                        {

                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {

                                saveImageToGallery();
                                dialog.dismiss();
                            }
                        })
                        .show();


                return true;
            }
        });
    }

    private void saveImageToGallery()
    {
        Observable.just(ConstantUtil.APP_NAME)
                .compose(bindToLifecycle())
                .compose(RxPermissions.getInstance(FuliFullPicActivity.this).ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .observeOn(Schedulers.io())
                .filter(new Func1<Boolean,Boolean>()
                {

                    @Override
                    public Boolean call(Boolean aBoolean)
                    {

                        return aBoolean;
                    }
                })
                .flatMap(new Func1<Boolean,Observable<Uri>>()
                {

                    @Override
                    public Observable<Uri> call(Boolean aBoolean)
                    {

                        return GlideDownloadImageUtil.saveImageToLocal(FuliFullPicActivity.this, url, title, ConstantUtil.PIC_TYPE_JPG);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .retry()
                .subscribe(new Action1<Uri>()
                {

                    @Override
                    public void call(Uri uri)
                    {

                        File appDir = new File(Environment.getExternalStorageDirectory(), ConstantUtil.FILE_DIR);
                        String msg = String.format("图片已保存至 %s 文件夹", appDir.getAbsolutePath());
                        Toast.makeText(FuliFullPicActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>()
                {

                    @Override
                    public void call(Throwable throwable)
                    {

                        Toast.makeText(FuliFullPicActivity.this, "保存失败,请重试", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /**
     * 保存图片到本地
     */
    private void saveImage()
    {

       RxMenuItem.clicks(mToolBar.getMenu().findItem(R.id.action_fuli_save))
               .compose(bindToLifecycle())
                .compose(RxPermissions.getInstance(FuliFullPicActivity.this).ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .observeOn(Schedulers.io())
                .filter(new Func1<Boolean,Boolean>()
                {

                    @Override
                    public Boolean call(Boolean aBoolean)
                    {

                        return aBoolean;
                    }
                })
                .flatMap(new Func1<Boolean,Observable<Uri>>()
                {

                    @Override
                    public Observable<Uri> call(Boolean aBoolean)
                    {

                        return GlideDownloadImageUtil.saveImageToLocal(FuliFullPicActivity.this, url, title, ConstantUtil.PIC_TYPE_JPG);
                    }
                })
                .map(new Func1<Uri,String>()
                {

                    @Override
                    public String call(Uri uri)
                    {

                        String msg = String.format("图片已保存至 %s 文件夹",
                                new File(Environment.getExternalStorageDirectory(), ConstantUtil.FILE_DIR)
                                        .getAbsolutePath());
                        return msg;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .retry()
                .subscribe(new Action1<String>()
                {

                    @Override
                    public void call(String s)
                    {

                        Toast.makeText(FuliFullPicActivity.this, s, Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>()
                {

                    @Override
                    public void call(Throwable throwable)
                    {

                        Toast.makeText(FuliFullPicActivity.this, "保存失败,请重试", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 分享图片
     */
    public void shareImage()
    {


        RxMenuItem.clicks(mToolBar.getMenu().findItem(R.id.action_fuli_share))
                .compose(bindToLifecycle())
                .compose(RxPermissions.getInstance(FuliFullPicActivity.this).ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .observeOn(Schedulers.io())
                .filter(new Func1<Boolean,Boolean>()
                {

                    @Override
                    public Boolean call(Boolean aBoolean)
                    {

                        return aBoolean;
                    }
                })
                .flatMap(new Func1<Boolean,Observable<Uri>>()
                {

                    @Override
                    public Observable<Uri> call(Boolean aBoolean)
                    {

                        return GlideDownloadImageUtil.saveImageToLocal(FuliFullPicActivity.this, url, title, ConstantUtil.PIC_TYPE_JPG);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .retry()
                .subscribe(new Action1<Uri>()
                {

                    @Override
                    public void call(Uri uri)
                    {

                        share(uri);
                    }
                }, new Action1<Throwable>()
                {

                    @Override
                    public void call(Throwable throwable)
                    {

                        Toast.makeText(FuliFullPicActivity.this, "分享失败,请重试", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 分享图片
     *
     * @param uri
     */
    private void share(Uri uri)
    {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, title));
    }

    protected void hideOrShowToolbar()
    {

        if (isHide)
        {
            //显示
            ImmersiveUtil.exit(this);
            mAppBarLayout.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(2))
                    .start();
            isHide = false;
        } else
        {
            //隐藏
            ImmersiveUtil.enter(this);
            mAppBarLayout.animate()
                    .translationY(-mAppBarLayout.getHeight())
                    .setInterpolator(new DecelerateInterpolator(2))
                    .start();
            isHide = true;
        }
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
}
