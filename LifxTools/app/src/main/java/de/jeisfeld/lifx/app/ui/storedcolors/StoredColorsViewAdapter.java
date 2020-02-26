package de.jeisfeld.lifx.app.ui.storedcolors;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.util.ColorRegistry;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.app.util.StoredColor;
import de.jeisfeld.lifx.lan.Light;

/**
 * Adapter for the RecyclerView that allows to sort devices.
 */
public class StoredColorsViewAdapter extends RecyclerView.Adapter<StoredColorsViewAdapter.MyViewHolder>
		implements StoredColorsItemMoveCallback.ItemTouchHelperContract {
	/**
	 * The list of stored colors as view data.
	 */
	private final List<StoredColor> mStoredColors;
	/**
	 * The list of device ids.
	 */
	private final List<Integer> mColorIds;
	/**
	 * The listener identifying start of drag.
	 */
	private StartDragListener mStartDragListener;
	/**
	 * A reference to the fragment.
	 */
	private final WeakReference<Fragment> mFragment;
	/**
	 * A reference to the recycler view.
	 */
	private final WeakReference<RecyclerView> mRecyclerView;

	/**
	 * Constructor.
	 *
	 * @param fragment     the calling fragment.
	 * @param recyclerView The recycler view.
	 */
	public StoredColorsViewAdapter(final Fragment fragment, final RecyclerView recyclerView) {
		mStoredColors = ColorRegistry.getInstance().getStoredColors();
		mColorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
		mFragment = new WeakReference<>(fragment);
		mRecyclerView = new WeakReference<>(recyclerView);
	}

	/**
	 * Set the listener identifying start of drag.
	 *
	 * @param startDragListener The listener.
	 */
	public void setStartDragListener(final StartDragListener startDragListener) {
		mStartDragListener = startDragListener;
	}

	@Override
	public final MyViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
		View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_stored_colors, parent, false);
		return new MyViewHolder(itemView);
	}

	@Override
	public final void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
		final StoredColor storedColor = mStoredColors.get(position);
		Light light = storedColor.getLight();
		if (light == null) {
			holder.mTitle.setText(storedColor.getName());
		}
		else {
			holder.mTitle.setText(storedColor.getName() + " (" + light.getLabel() + ")");
		}

		holder.mDragHandle.setOnTouchListener((view, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mStartDragListener.requestDrag(holder);
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				view.performClick();
			}
			return false;
		});

		holder.mDeleteButton.setOnClickListener(v -> {
			Fragment fragment = mFragment.get();
			if (fragment != null && fragment.getActivity() != null) {
				DialogUtil.displayConfirmationMessage(fragment.getActivity(), new ConfirmDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog) {
						ColorRegistry.getInstance().remove(storedColor);
						mStoredColors.remove(position);
						mColorIds.remove(position);
						RecyclerView recyclerView = mRecyclerView.get();
						if (recyclerView != null) {
							recyclerView.removeViewAt(position);
						}
						notifyItemRemoved(position);
						notifyItemRangeChanged(position, mStoredColors.size() - position);
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						// do nothing
					}
				}, null, R.string.button_delete, R.string.message_confirm_delete_color, storedColor.getName());
			}
		});

		holder.mApplyColorButton.setOnClickListener(v -> new SetColorTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, storedColor));
	}

	@Override
	public final int getItemCount() {
		return mStoredColors.size();
	}

	@Override
	public final void onRowMoved(final int fromPosition, final int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(mStoredColors, i, i + 1);
				Collections.swap(mColorIds, i, i + 1);
			}
		}
		else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(mStoredColors, i, i - 1);
				Collections.swap(mColorIds, i, i - 1);
			}
		}
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_device_ids, mColorIds);
		notifyItemMoved(fromPosition, toPosition);
	}

	@Override
	public final void onRowSelected(final MyViewHolder myViewHolder) {
		myViewHolder.mRowView.setBackgroundColor(Color.LTGRAY);

	}

	@Override
	public final void onRowClear(final MyViewHolder myViewHolder) {
		myViewHolder.mRowView.setBackgroundColor(Color.TRANSPARENT);

	}

	/**
	 * The view holder of the items.
	 */
	public class MyViewHolder extends RecyclerView.ViewHolder {
		/**
		 * The whole item.
		 */
		private final View mRowView;
		/**
		 * The title.
		 */
		private final TextView mTitle;
		/**
		 * The button to apply the color.
		 */
		private final ImageView mApplyColorButton;
		/**
		 * The image view.
		 */
		private final ImageView mDragHandle;
		/**
		 * The delete button.
		 */
		private final ImageView mDeleteButton;

		/**
		 * Constructor.
		 *
		 * @param itemView The item view.
		 */
		public MyViewHolder(final View itemView) {
			super(itemView);
			mRowView = itemView;
			mTitle = itemView.findViewById(R.id.textViewColorName);
			mDragHandle = itemView.findViewById(R.id.imageViewDragHandle);
			mDeleteButton = itemView.findViewById(R.id.imageViewDelete);
			mApplyColorButton = itemView.findViewById(R.id.imageViewApplyColor);
		}
	}

	/**
	 * A listener for starting the drag.
	 */
	public interface StartDragListener {
		/**
		 * Method for starting the drag.
		 *
		 * @param viewHolder The view Holder.
		 */
		void requestDrag(RecyclerView.ViewHolder viewHolder);
	}

	/**
	 * An async task for setting the color.
	 */
	private static final class SetColorTask extends AsyncTask<StoredColor, String, StoredColor> {
		@Override
		protected StoredColor doInBackground(final StoredColor... storedColors) {
			try {
				StoredColor storedColor = storedColors[0];
				storedColor.getLight().setColor(storedColor.getColor());
				return storedColor;
			}
			catch (IOException e) {
				Log.w(Application.TAG, e);
				return null;
			}
		}
	}

	/**
	 * Interface for an async task.
	 */
	protected interface AsyncExecutable {
		/**
		 * Execute the task.
		 */
		void execute();
	}

}
