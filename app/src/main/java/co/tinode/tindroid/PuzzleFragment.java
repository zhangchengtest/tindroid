package co.tinode.tindroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ContentInfoCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonElement;
import com.lwkandroid.imagepicker.data.ImageBean;
import com.lwkandroid.widget.ngv.DefaultNgvAdapter;
import com.lwkandroid.widget.ngv.NgvChildImageView;
import com.lwkandroid.widget.ngv.NineGridView;

import java.util.ArrayList;
import java.util.List;

import co.tinode.tindroid.media.VxCard;
import co.tinode.tindroid.stopwatch.ChronometerWithMillis;
import co.tinode.tindroid.util.GsonUtil;
import co.tinode.tindroid.util.HttpUrl;
import co.tinode.tindroid.util.OKHttp3Util;
import co.tinode.tindroid.util.PeerResultVO;
import co.tinode.tindroid.widgets.CircleProgressView;
import co.tinode.tindroid.widgets.HorizontalListDivider;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.NotConnectedException;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.model.ServerMessage;
import okhttp3.Request;

public class PuzzleFragment extends Fragment implements ActionMode.Callback, UiUtils.ProgressIndicator {
    private static final String TAG = "PuzzleFragment";

//    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1001;

    private Boolean mIsArchive;
    private Boolean mIsBanned;


    ChronometerWithMillis mChronometer;
    private NineGridView mNineGridView;

    private GlideDisplayer imageLoader;

    private DefaultNgvAdapter<ImageBean> mAdapter;
    private String ipStr;

    String chosenUrl = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mIsBanned = false;
        setHasOptionsMenu(!mIsBanned);

        View view = inflater.inflate(R.layout.fragment_puzzle,
                container, false);


        mNineGridView = view.findViewById(R.id.ninegridview);

        mChronometer =  view.findViewById(R.id.chronometer);

        mChronometer.setShowCentiseconds(false, true);

        Button mStopButton = view.findViewById(R.id.stop);

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapter.removeData();
                download(chosenUrl);
            }
        });

        //设置图片分割间距，默认8dp，默认对应attr属性中divider_line_size
        mNineGridView.setDividerLineSize(TypedValue.COMPLEX_UNIT_DIP, 2);
        //设置是否开启编辑模式，默认false，对应attr属性中enable_edit_mode
        mNineGridView.setEnableEditMode(false);
        //设置水平方向上有多少列，默认3，对应attr属性中horizontal_child_count
        mNineGridView.setHorizontalChildCount(3);
        //设置非编辑模式下，只有一张图片时的尺寸，默认都为0，当宽高都非0才生效，且不会超过NineGridView内部可用总宽度，对应attr属性中single_image_width、single_image_height
        mNineGridView.setSingleImageSize(TypedValue.COMPLEX_UNIT_DIP, 150, 200);
        imageLoader = new GlideDisplayer();
        mAdapter = new DefaultNgvAdapter<ImageBean>(100, imageLoader, getActivity());

        mNineGridView.setAdapter(mAdapter);

        download();

        mAdapter.setOnChildClickListener(new DefaultNgvAdapter.OnChildClickedListener<ImageBean>()
        {
            @Override
            public void onPlusImageClicked(ImageView plusImageView, int dValueToLimited)
            {

            }

            @Override
            public void onContentImageClicked(@NonNull int targetNum, String targetPath, @NonNull ContentInfoCompat source, int width, int height)
            {

                String tag2 = (String)source.getClip().getDescription().getLabel();
                int sourceNum = Integer.parseInt(tag2);
                NgvChildImageView sourceD = (NgvChildImageView)mNineGridView.getChildAt(sourceNum);
                String sourcePath = (String)source.getClip().getItemAt(0).getText();

                NgvChildImageView targetD = (NgvChildImageView)mNineGridView.getChildAt(targetNum);


                Log.e("ImagePicker", "come here");
                //target
                imageLoader.load(sourcePath, targetD,
                        width, height);
                //source
                imageLoader.load(targetPath, sourceD,
                        width, height);
            }

            @Override
            public void onImageDeleted(int position, ImageBean data)
            {
            }
        });


        return view;
    }


    private void download(String url ) {
//        final Resources resources = getResources();
//        final SharedPreferencesUtil sp = new SharedPreferencesUtil(activity);
        String token = "test-token";

        Log.i(TAG, "enter here " );
        ipStr = "http://api.punengshuo.com";
        Log.i(TAG, ipStr + HttpUrl.DOWNLOAD_REQUEST_URL +"?url="+url);
        OKHttp3Util.getAsyn(ipStr + HttpUrl.DOWNLOAD_REQUEST_URL +"?url="+url, token, new OKHttp3Util.ResultCallback<JsonElement>() {
            @Override
            public void onError(Request request, Exception e) {
                Log.e(TAG, "请求失败=" + e.toString());
            }

            @Override
            public void onResponse(JsonElement response) {
                Log.e(TAG,"成功--->" + response.toString());

                PeerResultVO result = GsonUtil.GsonToBean(response.toString(), PeerResultVO.class);

                List<ImageBean> list = new ArrayList<>();

                for(String str : result.getData().getPiecces()){
                    ImageBean imageBean = new ImageBean();
//                    imageBean.setImageId(imageId);
                    imageBean.setImagePath(str);
//                    imageBean.setLastModified(ImagePickerComUtils.isNotEmpty(lastModify) ? Long.valueOf(lastModify) : 0);
                    imageBean.setWidth(200);
                    imageBean.setHeight(200);
//                    imageBean.setFolderId(folderId);
                    list.add(imageBean);
                }

                mAdapter.addDataList(list);
                mChronometer.start();
            }
        });
        Log.i(TAG, "enter end " );
    }

    private void download( ) {
//        final Resources resources = getResources();
//        final SharedPreferencesUtil sp = new SharedPreferencesUtil(activity);
        String token = "test-token";

        Log.i(TAG, "enter here " );
        ipStr = "http://api.punengshuo.com";
        Log.i(TAG, ipStr + HttpUrl.DOWNLOAD_REQUEST );
        OKHttp3Util.getAsyn(ipStr + HttpUrl.DOWNLOAD_REQUEST, token, new OKHttp3Util.ResultCallback<JsonElement>() {
            @Override
            public void onError(Request request, Exception e) {
                Log.e(TAG, "请求失败=" + e.toString());
            }

            @Override
            public void onResponse(JsonElement response) {
                Log.e(TAG,"成功--->" + response.toString());

                PeerResultVO result = GsonUtil.GsonToBean(response.toString(), PeerResultVO.class);

                chosenUrl = result.getData().getUrl();
                List<ImageBean> list = new ArrayList<>();

                for(String str : result.getData().getPiecces()){
                    ImageBean imageBean = new ImageBean();
//                    imageBean.setImageId(imageId);
                    imageBean.setImagePath(str);
//                    imageBean.setLastModified(ImagePickerComUtils.isNotEmpty(lastModify) ? Long.valueOf(lastModify) : 0);
                    imageBean.setWidth(200);
                    imageBean.setHeight(200);
//                    imageBean.setFolderId(folderId);
                    list.add(imageBean);
                }

                mAdapter.addDataList(list);

            }
        });
        Log.i(TAG, "enter end " );
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) {
            return;
        }

        final ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setTitle(R.string.app_name);
        }
        view.findViewById(R.id.startNewChat).setOnClickListener(view1 -> {
            Intent intent = new Intent(activity, StartChatActivity.class);
            startActivity(intent);
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        Bundle bundle = getArguments();
        if (bundle != null) {
            mIsArchive = bundle.getBoolean(ChatsActivity.FRAGMENT_ARCHIVE, false);
            mIsBanned = bundle.getBoolean(ChatsActivity.FRAGMENT_BANNED, false);
        } else {
            mIsArchive = false;
            mIsBanned = false;
        }

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        // This is needed in order to accept video calls while the app is in the background.
        // It should be already granted to apps in playstore, but needed when developing.
//        if (!Settings.canDrawOverlays(activity)) {
//            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                    Uri.parse("package:" + activity.getPackageName()));
//            try {
//                //noinspection deprecation: registerForActivityResult does not work for this permission.
//                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
//            } catch (ActivityNotFoundException ignored) {
//                Log.w(TAG, "Unable to enable overlays, incoming calls limited.");
//                Toast.makeText(activity, R.string.voice_calls_limited, Toast.LENGTH_LONG).show();
//            }
//        }
    }

    // The registerForActivityResult does not work for this permission.
    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
//            final Activity activity = getActivity();
//            if (activity == null) {
//                return;
//            }
//
//            if (!Settings.canDrawOverlays(activity)) {
//                // User rejected request.
//                Log.w(TAG, "Incoming voice calls will be limited");
//                Toast.makeText(activity, R.string.voice_calls_limited, Toast.LENGTH_LONG).show();
//            }
//        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);


    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();

        inflater.inflate(R.menu.menu_chats, menu);
        menu.setGroupVisible(R.id.not_archive, !mIsArchive);
    }

    /**
     * This menu is shown when no items are selected
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final ChatsActivity activity = (ChatsActivity) getActivity();
        if (activity == null) {
            return true;
        }
        int id = item.getItemId();
        if (id == R.id.action_show_archive) {
            activity.showFragment(ChatsActivity.FRAGMENT_ARCHIVE, null);
            return true;
        } else if (id == R.id.action_settings) {
            activity.showFragment(ChatsActivity.FRAGMENT_ACCOUNT_INFO, null);
            return true;
        } else if (id == R.id.action_discover) {
            activity.showFragment(ChatsActivity.FRAGMENT_DISCOVER, null);
            return true;
        }else if (id == R.id.action_offline) {
            Cache.getTinode().reconnectNow(true, false, false);
        }
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_chats_selected, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {


        return true;
    }

    /**
     * This menu is shown when one or more items are selected from the list
     */
    //@Override
    @SuppressWarnings("unchecked")
    public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {

        Log.e(TAG, "Unknown menu action");
        return false;
    }

    // Confirmation dialog "Do you really want to do X?"
    private void showDeleteTopicsConfirmationDialog(final String[] topicNames) {
        final ChatsActivity activity = (ChatsActivity) getActivity();
        if (activity == null) {
            return;
        }

        final AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(activity);
        confirmBuilder.setNegativeButton(android.R.string.cancel, null);
        confirmBuilder.setMessage(R.string.confirm_delete_multiple_topics);
        confirmBuilder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    PromisedReply<ServerMessage> reply = null;
                    for (String name : topicNames) {
                        @SuppressWarnings("unchecked")
                        ComTopic<VxCard> t = (ComTopic<VxCard>) Cache.getTinode().getTopic(name);
                        try {
                            reply = t.delete(true).thenCatch(new PromisedReply.FailureListener<ServerMessage>() {
                                @Override
                                public PromisedReply<ServerMessage> onFailure(final Exception err) {
                                    activity.runOnUiThread(() -> {
                                        Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                                        Log.w(TAG, "Delete failed", err);
                                    });
                                    return null;
                                }
                            });
                        } catch (NotConnectedException ignored) {
                            Toast.makeText(activity, R.string.no_connection, Toast.LENGTH_SHORT).show();
                        } catch (Exception err) {
                            Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Delete failed", err);
                        }
                    }
                    // Wait for the last reply to resolve then update dataset.
                    if (reply != null) {
                        reply.thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                                datasetChanged();
                                return null;
                            }
                        });
                    }
                });
        confirmBuilder.show();
    }

    void datasetChanged() {
        toggleProgressIndicator(false);
    }

    // TODO: Add onBackPressed handing to parent Activity.
    public boolean onBackPressed() {

        return false;
    }

    @Override
    public void toggleProgressIndicator(final boolean on) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

    }
}
