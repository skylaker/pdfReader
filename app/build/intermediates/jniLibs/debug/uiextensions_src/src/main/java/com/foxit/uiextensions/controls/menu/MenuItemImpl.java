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
package com.foxit.uiextensions.controls.menu;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;


public class MenuItemImpl {
    private int mTag;
    public String text;
    public int iconId;
    public boolean enable;
    public MenuViewCallback callback;
    public View customView;

    private Context mContext;
    private TextView mText;
    private ImageView mImage;
    private View mView;
    private MenuViewCallback mCallback;
    private int mEventType;

    public static final int EVENT_BUTTON_CLICKED = 1;
    public static final int EVENT_ITEM_CLICKED = 2;

    public MenuItemImpl(Context context, int tag, String item_text, int imageID, MenuViewCallback callback) {
        mTag = tag;
        text = item_text;
        iconId = imageID;
        enable = true;
        this.callback = callback;

        mContext = context;
        mView = View.inflate(context, R.layout.view_menu_more_item, null);

        mText = (TextView) mView.findViewById(R.id.menu_more_item_tv);
        if (item_text == null) {
            mText.setVisibility(View.INVISIBLE);
        } else {
            mText.setText(item_text);
        }
        mImage = (ImageView) mView.findViewById(R.id.menu_more_item_bt);
        if (imageID == 0) {
            mImage.setVisibility(View.GONE);

        } else {
            mImage.setImageResource(imageID);
        }

        mCallback = callback;
        final MenuItemImpl itemSelf = this;

        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onClick(itemSelf);
                }
            }
        });
    }

    public MenuItemImpl(Context context, int tag, View customView) {
        mTag = tag;
        this.customView = customView;

        mContext = context;
        mView = View.inflate(context, R.layout.view_menu_more_item, null);

        LinearLayout ly = (LinearLayout) mView.getRootView();
        ly.removeAllViews();
        ly.addView(customView);
    }

    public void setDividerVisible(boolean visibly) {
        View divider = mView.findViewById(R.id.menu_more_item_divider);
        if (divider == null) return;
        if (visibly) {
            divider.setVisibility(View.VISIBLE);
        } else {
            divider.setVisibility(View.GONE);
        }
    }

    public boolean isCustomView() {
        if (customView != null) {
            return true;
        }

        return false;
    }

    public int getEventType() {
        return mEventType;
    }

    public View getView() {
        return mView;
    }

    public int getTag() {
        return mTag;
    }

    public void setEnable(boolean enable) {
        mView.setEnabled(enable);
        mText.setEnabled(enable);
        mImage.setEnabled(enable);
    }
}
