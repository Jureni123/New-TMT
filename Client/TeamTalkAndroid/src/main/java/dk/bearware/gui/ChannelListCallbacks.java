package dk.bearware.gui;

    import android.view.View;
    import dk.bearware.Channel;

    public interface ChannelListCallbacks {
        void onJoinChannel(Channel channel);
        void onItemClick(Object item);
        void onItemLongClick(Object item, View view);
        void openTextMessage(int userId);
    }
