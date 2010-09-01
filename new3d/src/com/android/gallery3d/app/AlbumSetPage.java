/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;

import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.ui.AdaptiveBackground;
import com.android.gallery3d.ui.AlbumSetView;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.HudMenu;
import com.android.gallery3d.ui.HeadUpDisplay;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;

public class AlbumSetPage extends ActivityState implements SlotView.SlotTapListener {
    private static final int CHANGE_BACKGROUND = 1;

    private static final int MARGIN_HUD_SLOTVIEW = 5;
    private static final int DATA_CACHE_SIZE = 256;

    private AdaptiveBackground mBackground;
    private AlbumSetView mAlbumSetView;
    private HudMenu mHudMenu;
    private HeadUpDisplay mHud;
    private SynchronizedHandler mHandler;

    private Bitmap mBgImages[];
    private int mBgIndex = 0;

    protected SelectionManager mSelectionManager;
    private AlbumSetDataAdapter mAlbumSetDataAdapter;

    private GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mBackground.layout(0, 0, right - left, bottom - top);
            mHud.layout(0, 0, right - left, bottom - top);

            int slotViewTop = mHud.getTopBarBottomPosition() + MARGIN_HUD_SLOTVIEW;
            int slotViewBottom = mHud.getBottomBarTopPosition()
                    - MARGIN_HUD_SLOTVIEW;

            mAlbumSetView.layout(0, slotViewTop, right - left, slotViewBottom);
        }
    };

    @Override
    public void onBackPressed() {
        if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    public void onSingleTapUp(int slotIndex) {
        if (!mSelectionManager.inSelectionMode()) {
            Bundle data = new Bundle();
            data.putInt(AlbumPage.KEY_BUCKET_INDEX, slotIndex);
            // uncomment the following line to test slideshow mode
            // mContext.getStateManager().startState(SlideshowPage.class, data);
            mContext.getStateManager().startState(AlbumPage.class, data);
        } else {
            long id = mAlbumSetDataAdapter.getMediaSet(slotIndex).getUniqueId();
            mSelectionManager.toggle(id);
            mAlbumSetView.invalidate();
        }
    }

    public void onLongTap(int slotIndex) {
        long id = mAlbumSetDataAdapter.getMediaSet(slotIndex).getUniqueId();
        mSelectionManager.toggle(id);
        mAlbumSetView.invalidate();
    }

    public AlbumSetPage() {}

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        initializeViews();
        intializeData();
        mHandler = new SynchronizedHandler(mContext.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case CHANGE_BACKGROUND:
                        changeBackground();
                        mHandler.sendEmptyMessageDelayed(
                                CHANGE_BACKGROUND, 3000);
                        break;
                }
            }
        };
    }

    @Override
    public void onPause() {
        mHandler.removeMessages(CHANGE_BACKGROUND);
        mHudMenu.onPause();
    }

    @Override
    public void onResume() {
        setContentPane(mRootPane);
        mHandler.sendEmptyMessage(CHANGE_BACKGROUND);
    }

    private void intializeData() {
        MediaSet mediaSet = mContext.getDataManager().getRootSet();
        mSelectionManager.setSourceMediaSet(mediaSet);
        mAlbumSetDataAdapter = new AlbumSetDataAdapter(mContext, mediaSet, DATA_CACHE_SIZE);
        mAlbumSetView.setModel(mAlbumSetDataAdapter);
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mContext, true);

        mBackground = new AdaptiveBackground();
        mRootPane.addComponent(mBackground);
        mAlbumSetView = new AlbumSetView(mContext, mSelectionManager);
        mAlbumSetView.setSlotTapListener(this);

        mRootPane.addComponent(mAlbumSetView);
        mHud = new HeadUpDisplay(mContext.getAndroidContext());
        mHudMenu = new HudMenu(mContext, mSelectionManager);
        mHud.setMenu(mHudMenu);
        mRootPane.addComponent(mHud);
        loadBackgroundBitmap(R.drawable.square,
                R.drawable.potrait, R.drawable.landscape);
        mBackground.setImage(mBgImages[mBgIndex]);
    }

    public void changeBackground() {
        mBackground.setImage(mBgImages[mBgIndex]);
        if (++mBgIndex == mBgImages.length) mBgIndex = 0;
    }

    private void loadBackgroundBitmap(int ... ids) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        mBgImages = new Bitmap[ids.length];
        Resources res = mContext.getResources();
        for (int i = 0, n = ids.length; i < n; ++i) {
            Bitmap bitmap = BitmapFactory.decodeResource(res, ids[i], options);
            mBgImages[i] = mBackground.getAdaptiveBitmap(bitmap);
            bitmap.recycle();
        }
    }
}
