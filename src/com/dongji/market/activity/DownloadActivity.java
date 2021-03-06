package com.dongji.market.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.ExpandableListView;

import com.dongji.market.R;
import com.dongji.market.adapter.DownloadAdapter;
import com.dongji.market.application.AppMarket;
import com.dongji.market.helper.AConstDefine;
import com.dongji.market.helper.DownloadUtils;
import com.dongji.market.pojo.DownloadEntity;
import com.dongji.market.service.DownloadService;
import com.umeng.analytics.MobclickAgent;

/**
 * 更新和安装页
 * 
 * @author yvon
 * 
 */
public class DownloadActivity extends Activity implements AConstDefine {
	private static final int EVENT_REQUEST_DATA = 1; // 请求与下载有关的数据
	private static final int EVENT_REFRESH_DATA = 2; // 刷新显示数据

	private ExpandableListView mExpandableListView;
	private View mLoadingView;
	private DownloadAdapter mAdapter;

	private MyHandler mHandler;

	private AppMarket mApp;

	private int locStep;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);
		mApp = (AppMarket) getApplication();
		initLoadingView();
		initHandler();
	}

	private void initLoadingView() {
		mLoadingView = findViewById(R.id.loadinglayout);
	}

	/**
	 * 请求数据（可更新，待安装，已忽略）
	 */
	private void initHandler() {
		HandlerThread mHandlerThread = new HandlerThread("");
		mHandlerThread.start();
		mHandler = new MyHandler(mHandlerThread.getLooper());
		mHandler.sendEmptyMessage(EVENT_REQUEST_DATA);
	}

	public class MyHandler extends Handler {
		MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case EVENT_REQUEST_DATA:// 请求数据
				final List<List<DownloadEntity>> childList = initData();// 获取子数据
				boolean hasDownloadData = childList != null ? childList.size() > 3 : false;
				final List<String> groupList = initGroupData(hasDownloadData);// 获取父数据
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						initViews(groupList, childList);
					}
				});
				break;
			case EVENT_REFRESH_DATA:// 刷新listview
				runOnUiThread(new Runnable() {

					@Override
					public void run() {// 每隔1秒刷新数据
						mAdapter.refreshAdapter();
						mHandler.sendEmptyMessageDelayed(EVENT_REFRESH_DATA, 1000L);
					}
				});
				break;
			}
		}
	}

	/**
	 * 初始化组数据
	 * 
	 * @return
	 */
	private List<List<DownloadEntity>> initData() {
		List<List<DownloadEntity>> adapterData = new ArrayList<List<DownloadEntity>>();
		List<DownloadEntity> installList = new ArrayList<DownloadEntity>();
		List<DownloadEntity> downloadingList = new ArrayList<DownloadEntity>();
		List<DownloadEntity> updatingList = new ArrayList<DownloadEntity>();
		List<DownloadEntity> ignoreList = new ArrayList<DownloadEntity>();
		if (DownloadService.mDownloadService != null) {
			List<DownloadEntity> downloadList = DownloadService.mDownloadService.getAllDownloadList();
			for (int i = 0; i < downloadList.size(); i++) {
				DownloadEntity entity = downloadList.get(i);
				if (entity.downloadType == TYPE_OF_COMPLETE) {// 待安装应用
					installList.add(entity);
				} else if (entity.downloadType == TYPE_OF_DOWNLOAD) {// 正在下载应用
					downloadingList.add(entity);
				} else if (entity.downloadType == TYPE_OF_UPDATE) {// 可更新应用
					DownloadUtils.setInstallDownloadEntity(this, entity);
					updatingList.add(entity);
				} else if (entity.downloadType == TYPE_OF_IGNORE) {// 已忽略应用
					DownloadUtils.setInstallDownloadEntity(this, entity);
					ignoreList.add(entity);
				}
			}
			if (downloadingList.size() > 0) {
				adapterData.add(downloadingList);
			}
			System.out.println("download size:" + downloadingList.size() + ", " + updatingList.size() + ", " + installList.size());
		}
		adapterData.add(updatingList);
		adapterData.add(installList);
		adapterData.add(ignoreList);
		return adapterData;
	}

	/**
	 * 初始化父数据，3组或4组
	 * 
	 * @param hasDownloadData
	 * @return
	 */
	private List<String> initGroupData(boolean hasDownloadData) {
		List<String> list = new ArrayList<String>();
		if (hasDownloadData) {
			list.add(getString(R.string.transferapk));// 正在传输
		}
		list.add(getString(R.string.updateapk));// 可更新
		list.add(getString(R.string.waitinstallapk));// 待安装
		list.add(getString(R.string.ignoreapk));// 忽略
		return list;
	}

	/**
	 * 初始化expandablelistview
	 * 
	 * @param groupList
	 * @param childList
	 */
	private void initViews(List<String> groupList, List<List<DownloadEntity>> childList) {
		mExpandableListView = (ExpandableListView) findViewById(R.id.exlvDownload);
		mExpandableListView.setChildDivider(getResources().getDrawable(R.drawable.list_divider));
		mExpandableListView.setGroupIndicator(null);
		mAdapter = new DownloadAdapter(this, childList, groupList, mHandler);
		mExpandableListView.setAdapter(mAdapter);
		int count = groupList.size() - 1;
		for (int i = 0; i < count; i++) {
			mExpandableListView.expandGroup(i);
		}
		mExpandableListView.collapseGroup(count);
		mLoadingView.setVisibility(View.GONE);
		mExpandableListView.setVisibility(View.VISIBLE);
		mHandler.sendEmptyMessageDelayed(EVENT_REFRESH_DATA, 1000L);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mHandler != null) {
			mHandler.removeMessages(EVENT_REFRESH_DATA);
		}
		if (mAdapter != null) {
			mAdapter.unregisterAllReceiver();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (getParent() == null) {
			return super.onCreateOptionsMenu(menu);
		} else {
			return getParent().onCreateOptionsMenu(menu);
		}
	}

	/**
	 * 按菜单键弹出设置popupwindow
	 */
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (getParent() == null) {
			return super.onMenuOpened(featureId, menu);
		} else {
			return getParent().onMenuOpened(featureId, menu);
		}
	}

	/**
	 * 使用3G下载是否提示过用户
	 * 
	 * @return
	 */
	public boolean is3GDownloadPromptUser() {
		return mApp.isIs3GDownloadPrompt();
	}

	/**
	 * 使用3G下载已提示用户
	 */
	public void set3GDownloadPromptUser() {
		mApp.setIs3GDownloadPrompt(true);
	}

	/**
	 * 点击actionbar空白栏操作
	 */
	void onToolBarClick() {
		if (mExpandableListView != null) {
			locStep = (int) Math.ceil(mExpandableListView.getFirstVisiblePosition() / AConstDefine.AUTO_SCRLL_TIMES);
			mExpandableListView.post(scrollToTop);
		}
	}

	Runnable scrollToTop = new Runnable() {

		@Override
		public void run() {
			if (mExpandableListView.getFirstVisiblePosition() > 0) {
				if (mExpandableListView.getFirstVisiblePosition() < AConstDefine.AUTO_SCRLL_TIMES) {
					mExpandableListView.setSelection(mExpandableListView.getFirstVisiblePosition() - 1);
				} else {
					mExpandableListView.setSelection(Math.max(mExpandableListView.getFirstVisiblePosition() - locStep, 0));
				}
				mExpandableListView.post(this);
			}
			return;
		}
	};
}
