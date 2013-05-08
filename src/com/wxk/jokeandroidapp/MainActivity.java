package com.wxk.jokeandroidapp;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wxk.tools.Common;
import com.wxk.tools.DownLoadHelper;
import com.wxk.tools.FilesHelper;
import com.wxk.tools.JokeListXMLContentHandler;

public class MainActivity extends Activity {

	private final String TAG = "MainActivity";
	private ListView listMain = null;
	private List<Map<String, String>> jokeList = null;
	private View loadingView = null;
	private boolean isLoading = false;
	private ProgressBar progressBar = null;
	private ProgressBar pb_loading = null;
	private JokeAdapter jokeAdapter = null;
	protected int listViewScrollState = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// setTheme(android.R.style.Theme_Translucent_NoTitleBar);

		LayoutInflater inflater = LayoutInflater.from(this);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// ���汾����
		// CheckVesion();
		listMain = (ListView) findViewById(R.id.list);
		// ���ϻ������ظ���Ľ�����
		progressBar = (ProgressBar) findViewById(R.id.titlebar_progress);
		pb_loading = (ProgressBar) findViewById(R.id.pb_loading);
		loadingView = inflater.inflate(R.layout.loading, null);
		loadingView.setVisibility(View.INVISIBLE);

		listMain.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				listViewScrollState = scrollState;
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
				if ((listViewScrollState == 2)
						&& (view.getLastVisiblePosition() >= totalItemCount - 1)) {
					CheckLoadMore();
				}
			}
		});
		listMain.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Log.d(TAG, "" + arg2 + "," + arg3);
				Map<String, String> jokeMap = jokeList.get(arg2);
				Intent intent = new Intent();
				intent.putExtra("jokecontent", jokeMap.get("content"));
				intent.putExtra("jokeTitle", jokeMap.get("title"));
				intent.putExtra("jokeID", jokeMap.get("id"));
				intent.putExtra("jokeImg", jokeMap.get("img"));
				intent.putExtra("haoxiao", jokeMap.get("haoxiao"));
				intent.putExtra("haoleng", jokeMap.get("haoleng"));
				intent.setClass(MainActivity.this, JokeDetailActivity.class);
				startActivity(intent);
			}
		});

		View listFooterView = inflater.inflate(R.layout.refresh_list_footer,
				null);

		listMain.addFooterView(listFooterView);
		jokeList = new ArrayList<Map<String, String>>();
		jokeAdapter = new JokeAdapter(jokeList, this);

		CheckLoadMore();
		listMain.setAdapter(jokeAdapter);
	}

	@Override
	/**
	 * ����Ҽ��˵�
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "�˳�");
		return true;
	}

	@Override
	/**
	 * �����Ҽ��˵��¼�
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == 0) {// �������˳���ť��ʾ�û��Ƿ��˳���
			Dialog_Eixt(MainActivity.this);
		}
		return false;
	}

	@Override
	/**
	 * �˳�ʱ�����˳���ʾ��
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if ((keyCode == event.KEYCODE_BACK || keyCode == 4 || keyCode == event.KEYCODE_HOME)
				&& event.getRepeatCount() == 0) {
			Dialog_Eixt(MainActivity.this);
		}
		return false;
	}

	/**
	 * �˳���ʾ��Ϣ
	 * 
	 * @param context
	 *            ��ǰ�ĵ�
	 */
	private static boolean showExit = false;

	private static void Dialog_Eixt(Context context) {
		if (!showExit) {
			Common.ShowDialog("�ٰ�һ���˳�����");
			showExit = true;
		} else {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	/**
	 * ��ȡ��������
	 */
	private Handler GetJokeList = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int res = msg.what;
			isLoading = false;
			if (res == 0) {
				loadingView.setVisibility(View.INVISIBLE);
				jokeAdapter.notifyDataSetChanged();
			} else if (res == 1) {
				loadingView.setVisibility(View.INVISIBLE);
				Common.ShowDialog("��������ʧ�ܣ���������������̫����\n�������¼���");
			}
			progressBar.setVisibility(View.GONE);
			pb_loading.setVisibility(View.GONE);
		}
	};

	private void CheckLoadMore() {

		if (isLoading) {
			return;
		} else {
			if (listMain.getFooterViewsCount() != 0) {
				loadingView.setVisibility(View.VISIBLE);
				listMain.setSelection(jokeList.size() - 1);
				LoadMoreItems();
			}
		}
	}

	private void LoadMoreItems() {
		isLoading = true;
		progressBar.setVisibility(View.VISIBLE);
		new Thread() {
			public void run() {

				try {

					DownLoadHelper downloadHelper = new DownLoadHelper();
					int totalCount = jokeList.size();
					int pageSize = 10;
					int current = totalCount / pageSize;
					current = current < 1 ? 1 : current + 1;
					String url = "http://www.52lxh.com/appinterface/getnewdatalist.aspx?current="
							+ current + "&pagesize=" + pageSize;
					String res = downloadHelper.DownLoad(url);
					System.out.println(res);
					try {
						SAXParserFactory factory = SAXParserFactory
								.newInstance();
						XMLReader reader = factory.newSAXParser()
								.getXMLReader();
						reader.setContentHandler(new JokeListXMLContentHandler(
								jokeList));
						reader.parse(new InputSource(new StringReader(res)));
						GetJokeList.sendEmptyMessage(0);
						progressBar.setVisibility(View.GONE);
					} catch (Exception e) {
						// TODO: handle exception
						GetJokeList.sendEmptyMessage(1);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private Handler UpdateNewVesion = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("�汾������ʾ")
					.setMessage("�����°汾���Ƿ���£�")
					.setPositiveButton("���ϸ���",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// TODO Auto-generated method stub
									downLoadApk();
								}
							})
					.setNegativeButton("�Ժ���˵",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// TODO Auto-generated method stub
								}
							}).show();
		}
	};

	private void CheckVesion() {
		new Thread() {
			public void run() {
				try {
					DownLoadHelper downloadHelper = new DownLoadHelper();
					String newVesion = downloadHelper
							.DownLoad("http://www.52lxh.com/appinterface/getnewvesion.aspx");
					if (Integer.parseInt(newVesion) > MApplication.currentVesion) {
						UpdateNewVesion.sendEmptyMessage(0);
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}.start();
	}

	/*
	 * �ӷ�����������APK
	 */
	private void downLoadApk() {
		final ProgressDialog pd; // �������Ի���
		pd = new ProgressDialog(this);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setMessage("�������ظ���");
		pd.show();
		new Thread() {
			@Override
			public void run() {
				try {
					File file = DownLoadHelper.getFileFromServer(
							"http://www.52lxh.com/downloadandroidapk.html", pd);
					// File file = new File(FilesHelper.GetPath("/52lxh/apk"),
					// "MainActivityNew.apk");
					installApk(file);
					pd.dismiss(); // �������������Ի���
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void installApk(File file) {
		Intent intent = new Intent();
		// ִ�ж���
		intent.setAction(Intent.ACTION_VIEW);
		// ִ�е���������
		intent.setDataAndType(Uri.fromFile(file),
				"application/vnd.android.package-archive");
		startActivity(intent);
	}
}
