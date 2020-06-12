package de.jeisfeld.lifx.app.alarms;

import javax.annotation.Nonnull;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Callback for handling drag of alarms.
 */
public class AlarmsItemMoveCallback extends ItemTouchHelper.Callback {
	/**
	 * The adapter.
	 */
	private final ItemTouchHelperContract mAdapter;

	/**
	 * Constructor.
	 *
	 * @param adapter The adapter.
	 */
	public AlarmsItemMoveCallback(final ItemTouchHelperContract adapter) {
		mAdapter = adapter;
	}

	@Override
	public final boolean isLongPressDragEnabled() {
		return false;
	}

	@Override
	public final boolean isItemViewSwipeEnabled() {
		return false;
	}

	@Override
	public void onSwiped(@Nonnull final RecyclerView.ViewHolder viewHolder, final int direction) {

	}

	@Override
	public final int getMovementFlags(@Nonnull final RecyclerView recyclerView, @Nonnull final RecyclerView.ViewHolder viewHolder) {
		int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		return makeMovementFlags(dragFlags, 0);
	}

	@Override
	public final boolean onMove(@Nonnull final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder,
								final RecyclerView.ViewHolder target) {
		mAdapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
		return true;
	}

	@Override
	public final void onSelectedChanged(final RecyclerView.ViewHolder viewHolder, final int actionState) {
		if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
			if (viewHolder instanceof AlarmsViewAdapter.MyViewHolder) {
				AlarmsViewAdapter.MyViewHolder myViewHolder = (AlarmsViewAdapter.MyViewHolder) viewHolder;
				mAdapter.onRowSelected(myViewHolder);
			}
		}
		super.onSelectedChanged(viewHolder, actionState);
	}

	@Override
	public final void clearView(@Nonnull final RecyclerView recyclerView, @Nonnull final RecyclerView.ViewHolder viewHolder) {
		super.clearView(recyclerView, viewHolder);
		if (viewHolder instanceof AlarmsViewAdapter.MyViewHolder) {
			AlarmsViewAdapter.MyViewHolder myViewHolder = (AlarmsViewAdapter.MyViewHolder) viewHolder;
			mAdapter.onRowClear(myViewHolder);
		}
	}

	/**
	 * Callback for row actions.
	 */
	public interface ItemTouchHelperContract {
		/**
		 * Callback called when row is moved.
		 *
		 * @param fromPosition start position.
		 * @param toPosition   end position.
		 */
		void onRowMoved(int fromPosition, int toPosition);

		/**
		 * Callback called when a row is selected.
		 *
		 * @param myViewHolder The holder of the selected view.
		 */
		void onRowSelected(AlarmsViewAdapter.MyViewHolder myViewHolder);

		/**
		 * Callback called when a row is deselected.
		 *
		 * @param myViewHolder The holder of the selected view.
		 */
		void onRowClear(AlarmsViewAdapter.MyViewHolder myViewHolder);
	}

}
