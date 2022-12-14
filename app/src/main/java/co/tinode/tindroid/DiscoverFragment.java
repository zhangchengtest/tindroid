package co.tinode.tindroid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import co.tinode.tindroid.media.VxCard;
import co.tinode.tinodesdk.MeTopic;
import co.tinode.tinodesdk.Tinode;

/**
 * Fragment for editing current user details.
 */
public class DiscoverFragment extends Fragment implements ChatsActivity.FormUpdatable {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) {
            return null;
        }
        // Inflate the fragment layout
        View fragment = inflater.inflate(R.layout.fragment_discover, container, false);
        final ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.action_discover);
        toolbar.setNavigationOnClickListener(v -> activity.getSupportFragmentManager().popBackStack());


        fragment.findViewById(R.id.bt_music).setOnClickListener(v ->{
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse("http://music.punengshuo.com");
            intent.setData(content_url);
            startActivity(intent);
        });
        fragment.findViewById(R.id.bt_plane).setOnClickListener(v ->{
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse("https://plane.punengshuo.com");
            intent.setData(content_url);
            startActivity(intent);
        });
        fragment.findViewById(R.id.bt_puzzle).setOnClickListener(v ->{
            final Tinode tinode = Cache.getTinode();
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse("http://game.punengshuo.com/#/puzzle/index?token="+tinode.getAuthToken());
            intent.setData(content_url);
            startActivity(intent);
        });

        fragment.findViewById(R.id.bt_tool).setOnClickListener(v ->{
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse("http://tools.punengshuo.com");
            intent.setData(content_url);
            startActivity(intent);
        });

        return fragment;
    }

    @Override
    public void onResume() {
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        final MeTopic<VxCard> me = Cache.getTinode().getMeTopic();

        if (me == null || activity == null) {
            return;
        }

        // Assign initial form values.
        updateFormValues(activity, me);

        super.onResume();
    }

    @Override
    public void updateFormValues(final FragmentActivity activity, final MeTopic<VxCard> me) {
        if (activity == null) {
            return;
        }

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        inflater.inflate(R.menu.menu_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            FragmentActivity activity = getActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return false;
            }

            ((ChatsActivity) activity).showFragment(ChatsActivity.FRAGMENT_ACC_PERSONAL, null);
            return true;
        }
        return false;
    }
}
