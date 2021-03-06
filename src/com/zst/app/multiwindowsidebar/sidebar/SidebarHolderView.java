package com.zst.app.multiwindowsidebar.sidebar;

import java.util.Map;
import java.util.Map.Entry;

import com.zst.app.multiwindowsidebar.Common;
import com.zst.app.multiwindowsidebar.R;
import com.zst.app.multiwindowsidebar.Util;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

@SuppressLint("ViewConstructor")
public class SidebarHolderView extends LinearLayout {
	
	//TODO cleanup code
	
	private final SidebarService mService;
	private final LayoutInflater mInflator;
	
	/* View Management */
	private final RelativeLayout mContentView;
	private final LinearLayout mHolderView;
	private final RelativeLayout mBarView;
	private final ImageView mTabView;
	private SidebarItemView[] mItemViews;
	
	
	/* Values for transferring touch events */
	private Rect mRect; // Used to check if the finger has moved outside of the sidebar rect
	private boolean mTransferTouchEventsToSidebarItemView;
	
	public SidebarHolderView(SidebarService service) {
		super(service);
		mInflator = LayoutInflater.from(service);
		mService = service;
		
		mInflator.inflate(R.layout.sidebar_main, this);
		mContentView = (RelativeLayout) findViewById(android.R.id.content);
		mBarView = (RelativeLayout) findViewById(android.R.id.background);
		mHolderView = (LinearLayout) findViewById(R.id.scroll_view_holder);

		final LinearLayout settings_menu = (LinearLayout) findViewById(R.id.ll_settings_menu);
		final ImageView iv_more_button = (ImageView) findViewById(R.id.more_button);
		final ImageView iv_create_group = (ImageView) findViewById(R.id.iv_create_group);
		final ImageView iv_edit = (ImageView) findViewById(R.id.iv_edit);
		final View.OnClickListener menu_listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.more_button:
					settings_menu.setVisibility(settings_menu.getVisibility() == View.VISIBLE ?
							View.GONE : View.VISIBLE);
					break;
				case R.id.iv_create_group:
					SidebarMenuOptions.createGroupFromTop2(getContext());
					break;
				case R.id.iv_edit:
					SidebarMenuOptions.launchEditApps(getContext());
					break;
				}
			}
		};
		settings_menu.setVisibility(View.GONE);
		iv_more_button.setOnClickListener(menu_listener);
		iv_create_group.setOnClickListener(menu_listener);
		iv_edit.setOnClickListener(menu_listener);
		
		if (mService.mBarOnRight) {
			mBarView.setBackgroundResource(R.drawable.bg_right);
			findViewById(android.R.id.button2).setVisibility(View.GONE);
			mTabView = (ImageView) findViewById(android.R.id.button1);
		} else {
			mBarView.setBackgroundResource(R.drawable.bg_left);
			findViewById(android.R.id.button1).setVisibility(View.GONE);
			mTabView = (ImageView) findViewById(android.R.id.button2);
		}
		mTabView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_DOWN:
				mTabView.setImageState(new int[] { android.R.attr.state_pressed }, false);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mTabView.setImageState(new int[] { android.R.attr.state_empty }, false);
				break;
			}
			mService.tabTouchEvent(event);	
			return true;
			}
		});
	}
	
	public void setTabSize(int dp) {
		RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)
				mTabView.getLayoutParams();
		param.width = Util.dp(dp, getContext());;
		mTabView.setLayoutParams(param);
	}
	
	public void setMarginFromTop(int top) {
		mTabView.setPadding(0, top, 0, 0);
	}
	
	public void addApps(final SharedPreferences pref, final PackageManager pm) {
		final Map<String, ?> map = pref.getAll();
		if (map.size() == 0) {
			Util.toast(mService, R.string.app_list_empty);
			return;
		}
		
		mHolderView.addView(new ProgressBar(mService));
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				mItemViews = new SidebarItemView[map.size()*2];

				for (Entry<String, ?> entry : map.entrySet()) {
					if (entry.getKey().contains(Common.SEPARATOR_GROUP)) {
						SidebarDualItemView item = new SidebarDualItemView(mService, mInflator) {
							@Override
							public void touchEventHelper(MotionEvent event, boolean long_press_verified) {
								itemViewTouchEventHelper(this, event, long_press_verified);
							}
						};
						try {
							String[] str = entry.getKey().split(Common.SEPARATOR_GROUP);

							ApplicationInfo info0 = pm.getApplicationInfo(str[0], 0);
							ApplicationInfo info1 = pm.getApplicationInfo(str[1], 0);
							// TODO cleanup
							final Drawable icon0 = info0.loadIcon(pm).mutate();
							final Drawable icon1 = info1.loadIcon(pm).mutate();
							icon0.setAlpha((int) (0.7f * 255));
							icon1.setAlpha((int) (0.7f * 255));
							item.setIcon(new LayerDrawable(new Drawable[] { icon0, icon1 }));
							
							item.setLabel(info0.loadLabel(pm) +" & "+ info1.loadLabel(pm));
							item.setPkg(str[0], str[1]);
						} catch (NameNotFoundException e) {
						}
						mItemViews[(Integer) entry.getValue()] = item;
					} else {
						SidebarItemView item = new SidebarItemView(mService, mInflator) {
							@Override
							public void touchEventHelper(MotionEvent event, boolean long_press_verified) {
								itemViewTouchEventHelper(this, event, long_press_verified);
							}
						};
						try {
							ApplicationInfo info = pm.getApplicationInfo(entry.getKey(), 0);
							item.setIcon(info.loadIcon(pm));
							item.setLabel(info.loadLabel(pm));
							item.setPkg(entry.getKey());
						} catch (NameNotFoundException e) {
						}
						mItemViews[(Integer) entry.getValue()] = item;
					}
				}
				
				mService.mHandler.post(new Runnable() {
					@Override
					public void run() {
						mHolderView.removeAllViews();
						for (SidebarItemView view : mItemViews) {
							if (view != null)
								mHolderView.addView(view);
						}
					}
				});
				
			}
		}).start();
	}

	private void itemViewTouchEventHelper(SidebarItemView item, MotionEvent event, boolean long_press_verified) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			setReceiveAllTouchEvents(true);
			break;
		case MotionEvent.ACTION_MOVE:
			if (long_press_verified) {
				mTransferTouchEventsToSidebarItemView = true;
				SidebarDraggedOutView view = SidebarDraggedOutView.getInstance(item);
				if (view.showView(item.getIcon())) {
					item.setToEmptyIcon(true);
					view.setPosition(event.getRawX(), event.getRawY(), false);
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			SidebarDraggedOutView view = SidebarDraggedOutView.getInstance(item);
			view.hideView();
			item.setToEmptyIcon(false);
			setReceiveAllTouchEvents(false);
		case MotionEvent.ACTION_CANCEL:
			break;
		}
	}
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_OUTSIDE:
			mService.mHandler.removeCallbacks(runnable);
			mService.hideBar();
			break;
		case MotionEvent.ACTION_MOVE:
			if (mTransferTouchEventsToSidebarItemView) {
				SidebarDraggedOutView view = SidebarDraggedOutView.getInstance(null);
				final boolean icon_inside_sidebar = mRect.contains(
						(int) event.getRawX(), (int) event.getRawY());
				view.setPosition(event.getRawX(), event.getRawY(), !icon_inside_sidebar);
				return true;
				// consume event so the listview won't scolll while we position the icon
			}
		case MotionEvent.ACTION_DOWN:
			int[] pos = new int[2];
			mBarView.getLocationOnScreen(pos);
			mRect = new Rect(
					pos[0],
					0,
					pos[0] + mBarView.getMeasuredWidth(),
					getResources().getDisplayMetrics().heightPixels);
			// Save the rect for checking later
			// http://stackoverflow.com/questions/6410200/
			mService.mHandler.removeCallbacks(runnable);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mTransferTouchEventsToSidebarItemView) {
				mTransferTouchEventsToSidebarItemView = false;
				SidebarDraggedOutView view = SidebarDraggedOutView.getInstance(null);
				final boolean icon_inside_sidebar = mRect.contains(
						(int) event.getRawX(), (int) event.getRawY());
				if (!icon_inside_sidebar) {
					// Finger is outside the sidebar.
					view.launch();
				}
				view.hideView();
			}
			mService.mHandler.postDelayed(runnable, Common.TIMEOUT_HIDE_SIDEBAR);
			break;
		}
		return super.dispatchTouchEvent(event);
	}
	
	final Runnable runnable = new Runnable () {
		@Override
		public void run() {
			mService.hideBar();
		}
	};
	
	public void animateView(boolean visible) {
		if (visible) {
			mService.addView(this);
			TranslateAnimation anim = new TranslateAnimation(
					Animation.RELATIVE_TO_PARENT,
					mService.mBarOnRight ? 1.0f : -1.0f,
					Animation.RELATIVE_TO_PARENT,
					0.0f,
					0, 0, 0, 0);
			anim.setDuration(mService.mAnimationTime);
			mContentView.startAnimation(anim);
			mService.mHandler.postDelayed(runnable, Common.TIMEOUT_HIDE_SIDEBAR);
		} else {
			TranslateAnimation anim = new TranslateAnimation(
					Animation.RELATIVE_TO_PARENT,
					0.0f,
					Animation.RELATIVE_TO_PARENT,
					mService.mBarOnRight ? 1.0f : -1.0f,
					0, 0, 0, 0);
			anim.setDuration(mService.mAnimationTime);
			anim.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					mService.safelyRemoveView(SidebarHolderView.this);
				}
			});
			mContentView.startAnimation(anim);
		}
	}
	public boolean setReceiveAllTouchEvents(boolean yes) {
		WindowManager.LayoutParams param = (WindowManager.LayoutParams) getLayoutParams();
		if (yes) {
	        param.flags = param.flags & ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
		} else {
	        param.flags = param.flags | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
		}
		try {
			mService.mWindowManager.updateViewLayout(this, param);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
