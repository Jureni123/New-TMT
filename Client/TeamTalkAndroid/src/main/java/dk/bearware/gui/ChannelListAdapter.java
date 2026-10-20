package dk.bearware.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Collections;
import java.util.Locale;
import java.util.Vector;

import dk.bearware.Channel;
import dk.bearware.ServerProperties;
import dk.bearware.TeamTalkBase;
import dk.bearware.User;
import dk.bearware.UserState;
import dk.bearware.backend.TeamTalkConstants;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.data.Preferences;
import dk.bearware.utils.PrefsHelper;
// TIDAK PERLU IMPORT UTILS DI SINI LAGI! KITA AKAN PAKAI ALAMAT LENGKAP SAAT DIGUNAKAN

public class ChannelListAdapter extends BaseAdapter {

    private static final int PARENT_CHANNEL_VIEW_TYPE = 0, 
                             CHANNEL_VIEW_TYPE = 1, 
                             USER_VIEW_TYPE = 2, 
                             INFO_VIEW_TYPE = 3, 
                             VIEW_TYPE_COUNT = 4;

    private final LayoutInflater inflater;
    private final UserProfileManager profileManager;
    private final TeamTalkService service;
    private final PrefsHelper prefs;
    private final MainActivity.ChannelListCallbacks callbacks;
    
    private Channel currentActiveChannel; 

    Vector<Channel> subchannels = new Vector<>();
    Vector<Channel> stickychannels = new Vector<>();
    Vector<User> currentusers = new Vector<>();

    public ChannelListAdapter(Context context, 
                              UserProfileManager mgr, 
                              TeamTalkService svc, 
                              PrefsHelper p, 
                              MainActivity.ChannelListCallbacks cb) {
        this.inflater = LayoutInflater.from(context);
        this.profileManager = mgr;
        this.service = svc;
        this.prefs = p;
        this.callbacks = cb;
    }

    public void setCurrentChannel(Channel chan) {
        this.currentActiveChannel = chan;
        notifyDataSetChanged();
    }

    @Override 
    public void notifyDataSetChanged() {
        subchannels.clear(); 
        stickychannels.clear(); 
        currentusers.clear();
        
        TeamTalkBase client = service.getTTInstance();
        int chanid = currentActiveChannel != null ? currentActiveChannel.nChannelID : client.getRootChannelID();
        
        if(currentActiveChannel != null) {
            // MENGGUNAKAN ALAMAT LENGKAP AGAR TIDAK ERROR IMPORT
            subchannels = dk.bearware.utils.Utils.getSubChannels(chanid, service.getChannels());
            stickychannels = dk.bearware.utils.Utils.getStickyChannels(chanid, service.getChannels());
            currentusers = dk.bearware.utils.Utils.getUsers(chanid, service.getUsers());
        } else {
            Channel root = service.getChannels().get(chanid);
            if(root != null) subchannels.add(root);
        }
        
        Collections.sort(subchannels, (c1, c2) -> c1.szName.compareToIgnoreCase(c2.szName));
        Collections.sort(stickychannels, (c1, c2) -> c1.szName.compareToIgnoreCase(c2.szName));
        Collections.sort(currentusers, (u1, u2) -> {
            if (prefs.get("movetalk_checkbox", true)) {
                boolean v1 = (u1.uUserState & UserState.USERSTATE_VOICE) != 0;
                boolean v2 = (u2.uUserState & UserState.USERSTATE_VOICE) != 0;
                if (v1 && !v2) return -1; 
                if (!v1 && v2) return 1;
            }
            // MENGGUNAKAN ALAMAT LENGKAP DI SINI JUGA
            return dk.bearware.utils.Utils.getDisplayName(inflater.getContext(), u1).compareToIgnoreCase(
                   dk.bearware.utils.Utils.getDisplayName(inflater.getContext(), u2));
        });
        super.notifyDataSetChanged();
    }

    @Override 
    public int getCount() {
        int count = currentusers.size() + subchannels.size() + stickychannels.size();
        if ((currentActiveChannel != null) && (currentActiveChannel.nParentID > 0)) count++;
        return count;
    }

    @Override 
    public Object getItem(int position) {
        if (position < stickychannels.size()) return stickychannels.get(position);
        position -= stickychannels.size();
        if (position < currentusers.size()) return currentusers.get(position);
        position -= currentusers.size();
        if ((currentActiveChannel != null) && (currentActiveChannel.nParentID > 0)) {
            if(position == 0) {
                Channel parent = service.getChannels().get(currentActiveChannel.nParentID);
                return parent != null ? parent : new Channel();
            }
            position--; 
        }
        return subchannels.get(position);
    }

    @Override 
    public long getItemId(int position) { return position; }

    @Override 
    public int getItemViewType(int position) {
        if (position < stickychannels.size()) return INFO_VIEW_TYPE;
        position -= stickychannels.size();
        if (position < currentusers.size()) return USER_VIEW_TYPE;
        position -= currentusers.size();
        if ((currentActiveChannel != null) && (currentActiveChannel.nParentID > 0)) {
            if (position == 0) return PARENT_CHANNEL_VIEW_TYPE;
            position--; 
        }
        return CHANNEL_VIEW_TYPE;
    }

    @Override 
    public int getViewTypeCount() { return VIEW_TYPE_COUNT; }

    @Override 
    public View getView(int position, View convertView, ViewGroup parent) {
        Object item = getItem(position);
        Context ctx = inflater.getContext();
        TeamTalkBase client = service.getTTInstance();
        
        if(item instanceof Channel channel) {
            switch (getItemViewType(position)) {
                case PARENT_CHANNEL_VIEW_TYPE:
                    if (convertView == null || convertView.findViewById(R.id.parentname) == null)
                        convertView = inflater.inflate(R.layout.item_channel_back, parent, false);
                    break;
                    
                case CHANNEL_VIEW_TYPE:
                    if (convertView == null || convertView.findViewById(R.id.channelname) == null)
                        convertView = inflater.inflate(R.layout.item_channel, parent, false);
                    
                    ImageView chanicon = convertView.findViewById(R.id.channelicon);
                    TextView name = convertView.findViewById(R.id.channelname);
                    TextView topic = convertView.findViewById(R.id.chantopic);
                    Button join = convertView.findViewById(R.id.join_btn);
                    
                    int iconRes = channel.bPassword ? R.drawable.channel_pink : R.drawable.channel_orange;
                    chanicon.setImageResource(iconRes);
                    
                    if(channel.nParentID == 0) {
                        ServerProperties srvprop = new ServerProperties();
                        client.getServerProperties(srvprop);
                        name.setText(srvprop.szServerName);
                    } else {
                        name.setText(channel.szName);
                    }
                    topic.setText(channel.szTopic);
                    
                    join.setOnClickListener(v -> callbacks.onJoinChannel(channel));
                    join.setEnabled(channel.nChannelID != client.getMyChannelID());
                    
                    if (channel.nMaxUsers > 0) {
                        // MENGGUNAKAN ALAMAT LENGKAP
                        int pop = dk.bearware.utils.Utils.getUsers(channel.nChannelID, service.getUsers()).size();
                        ((TextView)convertView.findViewById(R.id.population)).setText(
                            pop > 0 ? String.format(Locale.ROOT, "(%d)", pop) : ""
                        );
                    }
                    break;
                    
                case INFO_VIEW_TYPE:
                    if (convertView == null || convertView.findViewById(R.id.titletext) == null)
                        convertView = inflater.inflate(R.layout.item_info, parent, false);
                    ((TextView)convertView.findViewById(R.id.titletext)).setText(channel.szName);
                    ((TextView)convertView.findViewById(R.id.infodetails)).setText(channel.szTopic);
                    break;
            }
        } 
        else if(item instanceof User user) {
            if (convertView == null || convertView.findViewById(R.id.nickname) == null)
                convertView = inflater.inflate(R.layout.item_user, parent, false);
            
            ImageView usericon = convertView.findViewById(R.id.usericon);
            TextView nickname = convertView.findViewById(R.id.nickname);
            TextView status = convertView.findViewById(R.id.status);
            
            // MENGGUNAKAN ALAMAT LENGKAP UNTUK GETDISPLAYNAME
            String name = dk.bearware.utils.Utils.getDisplayName(ctx, user);
            nickname.setText(name);
            status.setText(user.szStatusMsg);
            
            boolean talking = (user.uUserState & UserState.USERSTATE_VOICE) != 0;
            boolean female = (user.nStatusMode & TeamTalkConstants.STATUSMODE_FEMALE) != 0;
            boolean away = (user.nStatusMode & TeamTalkConstants.STATUSMODE_AWAY) != 0;
            
            if(user.nUserID == client.getMyUserID()) talking = service.isVoiceTransmitting();
            
            int iconRes;
            if (talking) iconRes = female ? R.drawable.woman_green : R.drawable.man_green;
            else iconRes = female ? (away ? R.drawable.woman_orange : R.drawable.woman_blue) 
                                  : (away ? R.drawable.man_orange : R.drawable.man_blue);

            profileManager.loadAvatarSafe(user.nUserID, usericon, iconRes);

            Button sndmsg = convertView.findViewById(R.id.msg_btn);
            sndmsg.setOnClickListener(v -> callbacks.openTextMessage(user.nUserID));
        } 
        
        if (callbacks != null) {
            convertView.setOnClickListener(view -> callbacks.onItemClick(item));
            convertView.setOnLongClickListener(view -> {
                callbacks.onItemLongClick(item, view);
                return true;
            });
        }
        
        return convertView;
    } 
}
