package xyz.mustardcorp.secondscreen.custom;

/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder;

import java.util.ArrayList;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.misc.Util;

public class CustomDragAndDropAdapter
        extends RecyclerView.Adapter<CustomDragAndDropAdapter.MyViewHolder>
        implements DraggableItemAdapter<CustomDragAndDropAdapter.MyViewHolder> {
    private static final String TAG = "MyDraggableItemAdapter";
    private MyViewHolder viewHolder;

    // NOTE: Make accessible with short name
    private interface Draggable extends DraggableItemConstants {
    }

    private AbstractDataProvider mProvider;
    private Context mContext;

    public static class MyViewHolder extends AbstractDraggableItemViewHolder {
        public FrameLayout mContainer;
        public View mDragHandle;
        public Switch mSwitch;

        public MyViewHolder(View v) {
            super(v);
            mContainer = v.findViewById(R.id.container);
            mDragHandle = v.findViewById(R.id.drag_handle);
            mSwitch = v.findViewById(R.id.screen_toggle);
        }
    }

    public CustomDragAndDropAdapter(AbstractDataProvider dataProvider, Context context) {
        mProvider = dataProvider;
        mContext = context;

        // DraggableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return mProvider.getItem(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return mProvider.getItem(position).getViewType();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.layout_screen_page, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final AbstractDataProvider.Data item = mProvider.getItem(position);

        viewHolder = holder;

        // set text
        holder.mSwitch.setText(item.getText());
        holder.mSwitch.setChecked(item.isEnabled());
        holder.mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                if (b) Util.addViewIfNeeded(mContext, item.getKey(), 0);
                else Util.removeView(mContext, item.getKey());
                setItems();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mProvider.getCount();
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        Log.d(TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");

        if (fromPosition == toPosition) {
            return;
        }

        mProvider.moveItem(fromPosition, toPosition);

        setItems();

        notifyItemMoved(fromPosition, toPosition);
    }

    private void setItems() {
        ArrayList<String> keysToAdd = new ArrayList<>();

        for (int i = 0; i < mProvider.getCount(); i++) {
            AbstractDataProvider.Data item = mProvider.getItem(i);
            if (item.isEnabled()) {
                keysToAdd.add(item.getKey());
            }
        }

        Util.saveViews(mContext, keysToAdd);
    }

    @Override
    public boolean onCheckCanStartDrag(MyViewHolder holder, int position, int x, int y) {
//        // x, y --- relative from the itemView's top-left
//        final View containerView = holder.mContainer;
//        final View dragHandleView = holder.mDragHandle;
//
//        final int offsetX = containerView.getLeft() + (int) (ViewCompat.getTranslationX(containerView) + 0.5f);
//        final int offsetY = containerView.getTop() + (int) (ViewCompat.getTranslationY(containerView) + 0.5f);
//
////        return ViewUtils.hitTest(dragHandleView, x - offsetX, y - offsetY);
        return true;
    }

    @Override
    public ItemDraggableRange onGetItemDraggableRange(MyViewHolder holder, int position) {
        // no drag-sortable range specified
        return null;
    }

    @Override
    public boolean onCheckCanDrop(int draggingPosition, int dropPosition) {
        return true;
    }
}
