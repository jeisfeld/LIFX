package de.jeisfeld.lifx.app.scenes;

import javax.annotation.Nonnull;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Callback for handling drag of scenes.
 */
public class ScenesItemMoveCallback extends ItemTouchHelper.Callback {
        /** The adapter. */
        private final ItemTouchHelperContract mAdapter;

        /** Constructor.
         * @param adapter The adapter.
         */
        public ScenesItemMoveCallback(final ItemTouchHelperContract adapter) {
                mAdapter = adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
                return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
                return false;
        }

        @Override
        public void onSwiped(@Nonnull final RecyclerView.ViewHolder viewHolder, final int direction) {
        }

        @Override
        public int getMovementFlags(@Nonnull final RecyclerView recyclerView, @Nonnull final RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(@Nonnull final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder,
                                final RecyclerView.ViewHolder target) {
                mAdapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
        }

        @Override
        public void onSelectedChanged(final RecyclerView.ViewHolder viewHolder, final int actionState) {
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                        if (viewHolder instanceof ScenesViewAdapter.MyViewHolder) {
                                ScenesViewAdapter.MyViewHolder myViewHolder = (ScenesViewAdapter.MyViewHolder) viewHolder;
                                mAdapter.onRowSelected(myViewHolder);
                        }
                }
                super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public void clearView(@Nonnull final RecyclerView recyclerView, @Nonnull final RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (viewHolder instanceof ScenesViewAdapter.MyViewHolder) {
                        ScenesViewAdapter.MyViewHolder myViewHolder = (ScenesViewAdapter.MyViewHolder) viewHolder;
                        mAdapter.onRowClear(myViewHolder);
                }
        }

        /** Callback for row actions. */
        public interface ItemTouchHelperContract {
                void onRowMoved(int fromPosition, int toPosition);
                void onRowSelected(ScenesViewAdapter.MyViewHolder myViewHolder);
                void onRowClear(ScenesViewAdapter.MyViewHolder myViewHolder);
        }
}

