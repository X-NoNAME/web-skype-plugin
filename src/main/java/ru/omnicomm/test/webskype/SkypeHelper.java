/*
 * The MIT License
 *
 * Copyright 2015 Ivan (X-NoNAME) Kazakov <mail@x-noname.ru>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.omnicomm.test.webskype;

import com.samczsun.skype4j.Skype;
import com.samczsun.skype4j.chat.Chat;
import com.samczsun.skype4j.events.EventHandler;
import com.samczsun.skype4j.events.Listener;
import com.samczsun.skype4j.events.chat.message.MessageReceivedEvent;
import com.samczsun.skype4j.exceptions.SkypeException;
import com.samczsun.skype4j.formatting.Message;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 *
 * @author Ivan (X-NoNAME) Kazakov
 * @mailto mail@x-noname.ru
 */
public class SkypeHelper {
    
    private static Skype skype;
    private static String adminLogin;
    private static Map<String, Set<Chat>> chats = new HashMap<String, Set<Chat>>();
    
    public static boolean login(String sk_login, String sk_pass, String adminLogin) {
        SkypeHelper.adminLogin=adminLogin;
        if (skype != null) {
                try {
                    skype.logout();
                } catch (IOException ex) {
                    Logger.getLogger(WebSkypeBuilder.class.getName()).log(Level.FATAL, null, ex);
                }
            }
            if (sk_login == null || sk_pass == null || sk_login.isEmpty() || sk_pass.isEmpty()) {
                return false;
            }

            try {
                skype = Skype.login(sk_login, sk_pass);
            } catch (SkypeException ex) {
                Logger.getLogger(WebSkypeBuilder.class.getName()).log(Level.FATAL, null, ex);
                return false;
            }
        skype.getEventDispatcher().registerListener(new Listener() {
                    @EventHandler
                    public void onMessage(MessageReceivedEvent e) {
                        String msg = e.getMessage().getMessage().asPlaintext();
                        String command = msg.split(" ")[0];
                        if (e.getMessage().getSender().getUsername().equals(SkypeHelper.adminLogin)) {
                            preformCommands(CommandHandler.getAdminsOnly(), command, e, msg);
                        }
                        preformCommands(CommandHandler.getUserCommands(), command, e, msg);
                    }

                    public void preformCommands(Collection<Command> cmds, String command, MessageReceivedEvent e, String msg) {
                        for(Command cmd: cmds){
                            if(cmd.forCommand(command)){
                                cmd.perform(e,msg);
                            }
                        }
                    }

                });
        try {
            skype.subscribe();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(SkypeHelper.class.getName()).log(Level.FATAL, null, ex);
        }
        return false;
    }

    public static Map<String, Set<Chat>> getChats() {
        return chats;
    }

    public static boolean sendToChats(String sub, String msg, String groupsString) {
        if(sub==null)sub="";
        if(msg==null)msg="";
        if(groupsString==null || groupsString.isEmpty()) groupsString="default";
        String[] groups = groupsString.split(",");
        boolean res = false;
        for(String gr:groups){
            if(gr.isEmpty()) continue;
            for (Chat ch : chats.get(gr)) {
                try {
                    ch.sendMessage(Message.fromHtml("<b>" + sub + "</b>\n" + msg));
                    if(!res)res=true;
                } catch (SkypeException ex) {
                    Logger.getLogger(WebSkypeBuilder.class.getName()).log(Level.FATAL, null, ex);
                }
            }
        }
        return res;
    }

    public static void setAdmin(String adminLogin) {
        SkypeHelper.adminLogin=adminLogin;
    }
    
   

}
