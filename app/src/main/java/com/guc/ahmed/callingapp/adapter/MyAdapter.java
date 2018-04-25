//package com.guc.ahmed.callingapp.adapter;
//
//import android.support.v7.widget.CardView;
//import android.support.v7.widget.RecyclerView;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import com.guc.ahmed.callingapp.R;
//import com.guc.ahmed.callingapp.fragments.DestinationFragment;
//import com.guc.ahmed.callingapp.gucpoints.GucPlace;
//
//public class MyAdapter extends RecyclerView.Adapter<DestinationFragment.MyAdapter.ViewHolder> implements ItemTouchHelperAdapter {
//    private RecyclerView mRecyclerView;
//    private final OnStartDragListener mDragStartListener;
//
//    public MyAdapter(DestinationFragment destinationFragment){
//        mDragStartListener = destinationFragment;
//    }
//    // Create new views (invoked by the layout manager)
//    @Override
//    public DestinationFragment.MyAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
//        // create a new view
//        CardView v = (CardView) LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.destination_item, parent, false);
//
//        return new DestinationFragment.MyAdapter.ViewHolder(v);
//    }
//
//    @Override
//    public void onBindViewHolder(final DestinationFragment.MyAdapter.ViewHolder holder, int position) {
//        holder.destinationName.setText(chosenDestinations.get(position).getName());
//
//        holder.handleView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
//                    mDragStartListener.onStartDrag(holder);
//                }
//                return false;
//            }
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return chosenDestinations.size();
//    }
//
//    @Override
//    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
//        super.onAttachedToRecyclerView(recyclerView);
//        mRecyclerView = recyclerView;
//    }
//
//    @Override
//    public boolean onItemMove(int fromPosition, int toPosition) {
//        GucPlace place = chosenDestinations.get(fromPosition);
//        chosenDestinations.remove(fromPosition);
//        chosenDestinations.add(toPosition, place);
//        notifyItemMoved(fromPosition, toPosition);
//        return true;
//    }
//
//    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
//
//        private TextView destinationName;
//        private ImageView handleView;
//        private ImageView deleteView;
//
//        public ViewHolder(final View itemView) {
//            super(itemView);
//
//            deleteView = itemView.findViewById(R.id.delete);
//
//            deleteView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    chosenDestinations.remove(getAdapterPosition());
//                    notifyItemRemoved(getAdapterPosition());
//                    notifyItemRangeChanged(getAdapterPosition(),chosenDestinations.size());
//                    if(getItemCount()==0){
//                        mRecyclerView.setVisibility(View.INVISIBLE);
//                    }
//                }
//            });
//
//            destinationName = itemView.findViewById(R.id.destination_item_name);
//
//            handleView = (ImageView) itemView.findViewById(R.id.handle);
//        }
//
//        @Override
//        public void onClick(View view) {
//
//        }
//
//    }
//}
//
