/**
 * Copyright (C) 2003-2017, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit Mobile PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UIDialog;
import com.foxit.uiextensions.pdfreader.IPDFReader;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppTheme;

import java.util.ArrayList;

import static com.foxit.uiextensions.controls.propertybar.MultiLineBar.IML_ValueChangeListener;
import static com.foxit.uiextensions.controls.propertybar.MultiLineBar.TYPE_LOCKSCREEN;

public class ScreenLockModule implements Module {
    public static String MODULE_NAME_SCREENLOCK = "ScreenLock Module";
    private IPDFReader mReader;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public ScreenLockModule(PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_SCREENLOCK;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            UIExtensionsManager.Config config = ((UIExtensionsManager) mUiExtensionsManager).getModulesConfig();
            if (config != null && config.isLoadDefaultReader()) {
                mReader = ((UIExtensionsManager) mUiExtensionsManager).getPDFReader();
            }
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        initApplyValue();
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    private void initApplyValue() {
        setOrientation(2);
    }

    private void setOrientation(int orientation) {
        if (mReader == null || mReader.getMainFrame().getAttachedActivity() == null) {
            return;
        }
        switch (orientation) {
            case 0:
                mReader.getMainFrame().getAttachedActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 1:
                mReader.getMainFrame().getAttachedActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 2:
                mReader.getMainFrame().getAttachedActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                break;
        }
    }

    public IML_ValueChangeListener getScreenLockListener() {
        return mScreenLockListener;
    }

    private IML_ValueChangeListener mScreenLockListener = new IML_ValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (TYPE_LOCKSCREEN == type) {
                final ScreenLockDialog dialog = new ScreenLockDialog(mReader.getMainFrame().getAttachedActivity());
                dialog.setCurOption(getScreenLockPosition());
                dialog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (position == getScreenLockPosition()) {
                            return;
                        }
                        setOrientation(position);
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return TYPE_LOCKSCREEN;
        }
    };

    private int getScreenLockPosition() {
        if (mReader.getMainFrame().getAttachedActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            return 0;
        } else if (mReader.getMainFrame().getAttachedActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            return 1;
        } else {
            return 2;
        }
    }
}

class ScreenLockDialog extends UIDialog {
    private ArrayList<String> mOptionList;
    private int mCurOption = -1;
    private ListView mScreenLockList;
    private final Context mContext;

    public ScreenLockDialog(Context context) {
        super(context, R.layout.screen_lock_dialog, AppTheme.getDialogTheme(), AppDisplay.getInstance(context).getUITextEditDialogWidth());
        mContext = context;
        mScreenLockList = (ListView) mContentView.findViewById(R.id.rd_screen_lock_listview);
        if (AppDisplay.getInstance(mContext).isPad()) {
            usePadDimes();
        }
        setTitle(AppResource.getString(context, R.string.rv_screen_rotation_pad));
        mOptionList = new ArrayList<String>();
        mOptionList.add(AppResource.getString(mContext, R.string.rv_screen_rotation_pad_landscape));
        mOptionList.add(AppResource.getString(mContext, R.string.rv_screen_rotation_pad_portrait));
        mOptionList.add(AppResource.getString(mContext, R.string.rv_screen_rotation_pad_auto));
        mScreenLockList.setAdapter(screenLockAdapter);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mScreenLockList.setOnItemClickListener(listener);
    }

    public void setCurOption(int position) {
        mCurOption = position;
        screenLockAdapter.notifyDataSetChanged();
    }

    private void usePadDimes() {
        try {
            ((LinearLayout.LayoutParams) mTitleView.getLayoutParams()).leftMargin = AppDisplay.getInstance(mContext).dp2px(24);
            ((LinearLayout.LayoutParams) mTitleView.getLayoutParams()).rightMargin = AppDisplay.getInstance(mContext).dp2px(24);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static class ViewHolder {
        public TextView optionName;
        public ImageView checkedCircle;
    }

    private BaseAdapter screenLockAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mOptionList.size();
        }

        @Override
        public String getItem(int position) {
            return mOptionList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(mContext, R.layout.screen_lock_item, null);
                holder.optionName = (TextView) convertView.findViewById(R.id.rd_screen_lock_textview);
                holder.checkedCircle = (ImageView) convertView.findViewById(R.id.rd_screen_lock_imageview);
                if (AppDisplay.getInstance(mContext).isPad()) {
                    ((RelativeLayout.LayoutParams) holder.optionName.getLayoutParams()).leftMargin = (int) AppResource.getDimension(mContext, R.dimen.ux_horz_left_margin_pad);
                    ((RelativeLayout.LayoutParams) holder.checkedCircle.getLayoutParams()).rightMargin = (int) AppResource.getDimension(mContext, R.dimen.ux_horz_right_margin_pad);
                }
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.optionName.setText(mOptionList.get(position));
            if (position == mCurOption) {
                holder.checkedCircle.setImageResource(R.drawable.rd_circle_checked);
            } else {
                holder.checkedCircle.setImageResource(R.drawable.rd_circle_normal);
            }
            if (position == mOptionList.size() - 1) {
                convertView.setBackgroundResource(R.drawable.dialog_button_background_selector);
            } else {
                convertView.setBackgroundResource(R.drawable.rd_menu_item_selector);
            }
            return convertView;
        }
    };
}
