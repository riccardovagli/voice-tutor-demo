package com.example.uglylangtutor.voicetutor;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.uglylangtutor.R;

import java.util.List;

public class VoiceTutorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_TUTOR = 1;

    private final List<Message> messageList;
    private final OnItemClickListener listener;

    // Serve per sapere *quale messaggio* ha il segmento attivo
    private int highlightedMessageIndex = -1;
    private int activeStart = -1;
    private int activeEnd = -1;

    public interface OnItemClickListener {
        void onItemClick(String text);
    }

    public interface HighlightOffsetListener {
        void onOffsetCalculated(int position, int offsetY);
    }
    private HighlightOffsetListener offsetListener;

    public void setHighlightOffsetListener(HighlightOffsetListener listener) {
        this.offsetListener = listener;
    }

    public static class Message {
        public final boolean fromUser;
        public final String text;

        public Message(boolean fromUser, String text) {
            this.fromUser = fromUser;
            this.text = text;
        }
    }

    public VoiceTutorAdapter(List<Message> messages, OnItemClickListener listener) {
        this.messageList = messages;
        this.listener = listener;
    }



    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).fromUser ? VIEW_TYPE_USER : VIEW_TYPE_TUTOR;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_TYPE_USER) ? R.layout.item_user_message : R.layout.item_tutor_message;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        ((MessageViewHolder) holder).bind(
                message.text,
                position,
                highlightedMessageIndex,
                activeStart,
                activeEnd,
                listener
        );
    }

    /*
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        //((MessageViewHolder) holder).bind(message.text, listener);
        ((MessageViewHolder) holder).bind(
                message.text,
                position,
                activeStart,
                activeEnd,
                highlightedMessageIndex,
                listener
        );

    }
     */

    /*
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        ((MessageViewHolder) holder).bind(message.text, listener);
    }
     */


    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public MessageViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.message_text);
        }

        public void bind(final String text, final int position, final int highlightedIndex, final int activeStart, final int activeEnd, final OnItemClickListener listener) {
            String visibleText = text.replaceAll("\\\\[a-z]{2}", "");
            //String visibleText = text;

            if (position == highlightedIndex && activeStart >= 0 && activeEnd > activeStart && activeEnd <= text.length()) {
                int shiftStart = activeStart;
                int shiftEnd = activeEnd;

                SpannableString spannable = new SpannableString(visibleText);
                spannable.setSpan(
                        new BackgroundColorSpan(0xFFFFFF00),
                        shiftStart,
                        shiftEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                textView.setText(spannable);
                textView.post(() -> {
                    Layout layout = textView.getLayout();
                    if (layout != null && position == highlightedIndex) {
                        int line = layout.getLineForOffset(shiftStart);
                        int offsetY = layout.getLineTop(line);

                        RecyclerView recyclerView = (RecyclerView) itemView.getParent();
                        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
                        if (lm instanceof LinearLayoutManager) {
                            ((LinearLayoutManager) lm).scrollToPositionWithOffset(position, offsetY * -1);
                        }
                    }
                });

            } else {
                textView.setText(visibleText);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(text));
        }

        /*
        public void bind(final String text, final OnItemClickListener listener) {
            textView.setText(text);
            itemView.setOnClickListener(v -> listener.onItemClick(text));
        }
         */

        /*
        public void OLD_bind(final String text, final int position, final int activeStart, final int activeEnd, final int highlightedIndex, final OnItemClickListener listener) {
            if (position == highlightedIndex && activeStart >= 0 && activeEnd > activeStart && activeEnd <= text.length()) {
                SpannableString spannable = new SpannableString(text);
                spannable.setSpan(
                        new BackgroundColorSpan(0xFFFFFF00),
                        activeStart,
                        activeEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                textView.setText(spannable);

                textView.post(() -> {
                    Layout layout = textView.getLayout();
                    if (layout != null && activeStart >= 0) {
                        int line = layout.getLineForOffset(activeStart);
                        int y = layout.getLineTop(line);

                        int[] location = new int[2];
                        textView.getLocationOnScreen(location);

                        int absoluteY = location[1] + y;
                        int screenHeight = textView.getResources().getDisplayMetrics().heightPixels;

                        if (absoluteY < 0 || absoluteY > screenHeight - 200) {
                            View recyclerView = findRecyclerView(textView);
                            if (recyclerView instanceof RecyclerView) {
                                ((RecyclerView) recyclerView).smoothScrollBy(0, absoluteY - screenHeight / 2);
                            }
                        }
                    }
                });
            } else {
                textView.setText(text);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(text));
        }
         */

    }

    private static View findRecyclerView(View view) {
        View parent = (View) view.getParent();
        while (parent != null && !(parent instanceof RecyclerView)) {
            if (!(parent.getParent() instanceof View)) break;
            parent = (View) parent.getParent();
        }
        return parent;
    }


    public void highlightSegment(int messageIndex, int start, int end) {
        this.highlightedMessageIndex = messageIndex;
        this.activeStart = start;
        this.activeEnd = end;
        notifyItemChanged(messageIndex);
    }

    public void resetHighlight() {
        if (highlightedMessageIndex != -1) {
            int oldIndex = highlightedMessageIndex;
            highlightedMessageIndex = -1;
            activeStart = -1;
            activeEnd = -1;
            notifyItemChanged(oldIndex);
        }
    }


    public int getMessageIndexByText(String text) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).text.equals(text) && !messageList.get(i).fromUser) {
                return i;
            }
        }
        return -1;
    }

}
