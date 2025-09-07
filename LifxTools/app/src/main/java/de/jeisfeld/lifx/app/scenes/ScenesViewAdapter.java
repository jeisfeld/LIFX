package de.jeisfeld.lifx.app.scenes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.PreferenceUtil;

/**
 * Adapter for scenes list.
 */
public class ScenesViewAdapter extends RecyclerView.Adapter<ScenesViewAdapter.MyViewHolder>
		implements ScenesItemMoveCallback.ItemTouchHelperContract {

	private final List<Scene> mScenes;
	private final List<Integer> mSceneIds;
	private final WeakReference<Fragment> mFragment;
	private StartDragListener mStartDragListener;

	/**
	 * Constructor.
	 */
	public ScenesViewAdapter(final Fragment fragment, final RecyclerView recyclerView) {
		mScenes = SceneRegistry.getInstance().getScenes();
		mSceneIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_scene_ids);
		mFragment = new WeakReference<>(fragment);
		setHasStableIds(true);
	}

	/**
	 * Set drag listener.
	 */
	public void setStartDragListener(final StartDragListener startDragListener) {
		mStartDragListener = startDragListener;
	}

	@Override
	public MyViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_scenes, parent, false);
		return new MyViewHolder(view);
	}

	@Override
	public void onBindViewHolder(final MyViewHolder holder, final int position) {
		holder.mScene = mScenes.get(position);
		holder.mTextViewSceneName.setText(holder.mScene.getName());
		holder.mTextViewSceneName.setOnClickListener(v -> SceneConfigurationFragment.navigate(mFragment.get(), holder.mScene.getId()));

		holder.mImageViewDelete.setOnClickListener(v -> DialogUtil.displayConfirmationMessage(mFragment.get().requireActivity(), dialog -> {
			SceneRegistry.getInstance().remove(holder.mScene);
			int pos = holder.getAdapterPosition();
			mScenes.remove(pos);
			mSceneIds.remove(pos);
			notifyItemRemoved(pos);
			notifyItemRangeChanged(pos, mScenes.size() - pos);
		}, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_scene, holder.mScene.getName()));

		holder.mImageViewDragHandle.setOnTouchListener((v, event) -> {
			if (mStartDragListener != null) {
				mStartDragListener.requestDrag(holder);
			}
			return false;
		});
	}

	@Override
	public int getItemCount() {
		return mScenes.size();
	}

	@Override
	public long getItemId(final int position) {
		return mScenes.get(position).getId();
	}

	@Override
	public void onRowMoved(final int fromPosition, final int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(mScenes, i, i + 1);
				Collections.swap(mSceneIds, i, i + 1);
			}
		}
		else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(mScenes, i, i - 1);
				Collections.swap(mSceneIds, i, i - 1);
			}
		}
		notifyItemMoved(fromPosition, toPosition);
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_scene_ids, mSceneIds);
	}

	@Override
	public void onRowSelected(final MyViewHolder myViewHolder) {
		// do nothing
	}

	@Override
	public void onRowClear(final MyViewHolder myViewHolder) {
		// do nothing
	}

	/**
	 * Listener to start drag.
	 */
	public interface StartDragListener {
		void requestDrag(RecyclerView.ViewHolder viewHolder);
	}

	/**
	 * View holder.
	 */
	public static class MyViewHolder extends RecyclerView.ViewHolder {
		private final TextView mTextViewSceneName;
		private final ImageView mImageViewDelete;
		private final ImageView mImageViewDragHandle;
		private Scene mScene;

		MyViewHolder(final View view) {
			super(view);
			mTextViewSceneName = view.findViewById(R.id.textViewSceneName);
			mImageViewDelete = view.findViewById(R.id.imageViewDelete);
			mImageViewDragHandle = view.findViewById(R.id.imageViewDragHandle);
		}
	}
}

