package co.tinode.tindroid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tindroid.widgets.CircleProgressView;
import co.tinode.tindroid.widgets.HorizontalListDivider;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.NotConnectedException;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.model.ServerMessage;

public class ChatsFragment extends Fragment implements ActionMode.Callback, UiUtils.ProgressIndicator {
    private static final String TAG = "ChatsFragment";

//    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1001;

    private Boolean mIsArchive;
    private Boolean mIsBanned;
    private boolean mSelectionMuted;

    // "Loading..." indicator.
    private CircleProgressView mProgressView;

    private ChatsAdapter mAdapter = null;
    private SelectionTracker<String> mSelectionTracker = null;
    private ActionMode mActionMode = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            mIsArchive = args.getBoolean(ChatsActivity.FRAGMENT_ARCHIVE, false);
            mIsBanned = args.getBoolean(ChatsActivity.FRAGMENT_BANNED, false);
        } else {
            mIsArchive = false;
            mIsBanned = false;
        }

        setHasOptionsMenu(!mIsBanned);

        return inflater.inflate(mIsArchive ? R.layout.fragment_archive : R.layout.fragment_chats,
                container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) {
            return;
        }

        final ActionBar bar = activity.getSupportActionBar();
        if (mIsArchive || mIsBanned) {
            if (bar != null) {
                bar.setDisplayHomeAsUpEnabled(true);
                bar.setTitle(mIsArchive ? R.string.archived_chats : R.string.blocked_contacts);
                ((Toolbar) activity.findViewById(R.id.toolbar)).setNavigationOnClickListener(v ->
                        activity.getSupportFragmentManager().popBackStack());
            }
        } else {
            if (bar != null) {
                bar.setDisplayHomeAsUpEnabled(false);
                bar.setTitle(R.string.app_name);
            }
            view.findViewById(R.id.startNewChat).setOnClickListener(view1 -> {
                Intent intent = new Intent(activity, StartChatActivity.class);
                startActivity(intent);
            });
        }

        RecyclerView rv = view.findViewById(R.id.chat_list);
        rv.setLayoutManager(new LinearLayoutManager(activity));
        rv.setHasFixedSize(true);
        rv.addItemDecoration(new HorizontalListDivider(activity));
        mAdapter = new ChatsAdapter(activity, topicName -> {
            if (mActionMode != null || mIsBanned || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            Intent intent = new Intent(activity, MessageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("topic", topicName);
            activity.startActivity(intent);
        }, t -> (t.isArchived() == mIsArchive) && (t.isJoiner() != mIsBanned));
        rv.setAdapter(mAdapter);

        // Progress indicator.
        mProgressView = view.findViewById(R.id.progressCircle);

        mSelectionTracker = new SelectionTracker.Builder<>(
                "contacts-selection",
                rv,
                new ChatsAdapter.ContactKeyProvider(mAdapter),
                new ChatsAdapter.ContactDetailsLookup(rv),
                StorageStrategy.createStringStorage())
                .build();

        mSelectionTracker.onRestoreInstanceState(savedInstanceState);

        mAdapter.setSelectionTracker(mSelectionTracker);
        mSelectionTracker.addObserver(new SelectionTracker.SelectionObserver<String>() {
            @Override
            public void onSelectionChanged() {
                super.onSelectionChanged();
                if (mSelectionTracker.hasSelection() && mActionMode == null) {
                    mActionMode = activity.startSupportActionMode(ChatsFragment.this);
                } else if (!mSelectionTracker.hasSelection() && mActionMode != null) {
                    mActionMode.finish();
                    mActionMode = null;
                }
                if (mActionMode != null) {
                    mActionMode.setTitle("" + mSelectionTracker.getSelection().size());
                }
            }

            @Override
            public void onItemStateChanged(@NonNull String topicName, boolean selected) {
                int after = mSelectionTracker.getSelection().size();
                int before = selected ? after - 1 : after + 1;
                if (after == 1) {
                    ComTopic topic = (ComTopic) Cache.getTinode().getTopic(topicName);
                    if (topic != null) {
                        mSelectionMuted = topic.isMuted();
                    }
                }
                if (mActionMode != null) {
                    if ((before > 1) != (after > 1)) {
                        mActionMode.invalidate();
                    }
                }
            }
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

        mAdapter.resetContent(activity);

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

        if (mSelectionTracker != null) {
            mSelectionTracker.onSaveInstanceState(outState);
        }
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
        mSelectionTracker.clearSelection();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean single = mSelectionTracker.getSelection().size() == 1;

        if (mIsBanned) {
            menu.setGroupVisible(R.id.single_selection, false);
            menu.findItem(R.id.action_unblock).setVisible(single);
            return true;
        }

        boolean deleted = false;
        if (single) {
            final Selection<String> selection = mSelectionTracker.getSelection();
            //noinspection unchecked
            ComTopic<VxCard> topic = (ComTopic<VxCard>) Cache.getTinode().getTopic(selection.iterator().next());
            deleted = topic != null && topic.isDeleted();
        }

        if (deleted) {
            menu.setGroupVisible(R.id.single_selection, false);
            menu.findItem(R.id.action_unblock).setVisible(false);
        } else {
            menu.setGroupVisible(R.id.single_selection, single);

            if (single) {
                menu.findItem(R.id.action_mute).setVisible(!mSelectionMuted);
                menu.findItem(R.id.action_unmute).setVisible(mSelectionMuted);

                menu.findItem(R.id.action_archive).setVisible(!mIsArchive);
                menu.findItem(R.id.action_unarchive).setVisible(mIsArchive);
            }
        }

        return true;
    }

    /**
     * This menu is shown when one or more items are selected from the list
     */
    //@Override
    @SuppressWarnings("unchecked")
    public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
        final ChatsActivity activity = (ChatsActivity) getActivity();
        if (activity == null) {
            return false;
        }

        final Selection<String> selection = mSelectionTracker.getSelection();
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            String[] topicNames = new String[selection.size()];
            int i = 0;
            for (String name : selection) {
                topicNames[i++] = name;
            }
            showDeleteTopicsConfirmationDialog(topicNames);
            // Close CAB
            mode.finish();
            return true;
        } else if (id == R.id.action_mute || id == R.id.action_unmute) {
            // Muting is possible regardless of subscription status.
            if (!selection.isEmpty()) {
                final ComTopic<VxCard> topic =
                        (ComTopic<VxCard>) Cache.getTinode().getTopic(selection.iterator().next());
                topic.updateMuted(!topic.isMuted())
                        .thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                                datasetChanged();
                                return null;
                            }
                        })
                        .thenCatch(new PromisedReply.FailureListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onFailure(final Exception err) {
                                activity.runOnUiThread(() -> {
                                    Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                                    Log.w(TAG, "Muting failed", err);
                                });
                                return null;
                            }
                        });
            }
            mode.finish();
            return true;
        } else if (id == R.id.action_archive || id == R.id.action_unarchive) {
            // Archiving is possible regardless of subscription status.
            final ComTopic<VxCard> topic =
                    (ComTopic<VxCard>) Cache.getTinode().getTopic(selection.iterator().next());
            topic.updateArchived(!topic.isArchived())
                    .thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                        @Override
                        public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                            mAdapter.resetContent(activity);
                            return null;
                        }
                    })
                    .thenCatch(new PromisedReply.FailureListener<ServerMessage>() {
                        @Override
                        public PromisedReply<ServerMessage> onFailure(final Exception err) {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "Archiving failed", err);
                            });
                            return null;
                        }
                    });

            mode.finish();
            return true;
        } else if (id == R.id.action_unblock) {
            final ComTopic<VxCard> topic =
                    (ComTopic<VxCard>) Cache.getTinode().getTopic(selection.iterator().next());
            topic.subscribe().thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                @Override
                public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                    mAdapter.resetContent(activity);
                    return null;
                }
            }).thenCatch(new PromisedReply.FailureListener<ServerMessage>() {
                @Override
                public PromisedReply<ServerMessage> onFailure(final Exception err) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Failed to unban", err);
                    });
                    return null;
                }
            });
            topic.leave();
            mode.finish();
            return true;
        }

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
        mAdapter.resetContent(getActivity());
    }

    // TODO: Add onBackPressed handing to parent Activity.
    public boolean onBackPressed() {
        if (mSelectionTracker.hasSelection()) {
            mSelectionTracker.clearSelection();
        } else {
            return true;
        }
        return false;
    }

    @Override
    public void toggleProgressIndicator(final boolean on) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        activity.runOnUiThread(() -> {
            if (on) {
                mProgressView.show();
            } else {
                mProgressView.hide();
            }
        });
    }
}
