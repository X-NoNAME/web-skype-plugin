/*
 *
 * Copyright 2015 Ivan (X-NoNAME) Kazakov <mail@x-noname.ru>.
 */

package ru.omnicomm.test.webskype;

import com.samczsun.skype4j.chat.Chat;
import com.samczsun.skype4j.events.chat.message.MessageReceivedEvent;
import com.samczsun.skype4j.exceptions.SkypeException;
import com.samczsun.skype4j.formatting.Message;
import com.samczsun.skype4j.formatting.Text;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 *
 * @author Ivan (X-NoNAME) Kazakov
 * @mailto mail@x-noname.ru
 */
public class CommandHandler {

    public static Collection<Command> getAdminsOnly() {
        if(!adminCommands.isEmpty()) return adminCommands;
        
        adminCommands.add(new BaseCommand("\\tag", " [tag1,tag2] - если задан параметр,"
                + " то добавить тег к чату иначе отобразить список всех тегов.") {
            @Override
            public void perform(MessageReceivedEvent e, String msg) {
                String[] params = msg.split(" ",2);
                Map<String, Set<Chat>> chats = SkypeHelper.getChats();
                if(params.length==1){
                    String r = "Список всех тегов: \n";
                    for(String key:chats.keySet()){
                        r+="  "+key+": \n";
                        Set<Chat> keyChats = chats.get(key);
                        if(keyChats==null)continue;
                        for(Chat ch:keyChats){
                            if(e.getChat().equals(ch)){
                                r+="    <b>"+ch.getIdentity()+"</b> - текущий\n";
                            }else {
                                r+="    "+ch.getIdentity()+"\n";
                            }
                        }
                    }
                    sendResponse(e, r);
                }else {
                    String[] tags = params[1].split(",");
                    for(String tg:tags){
                        tg=tg.trim();
                        if(tg.isEmpty()) continue;
                        Set<Chat> tChat = chats.get(tg);
                        if(tChat==null){
                            tChat = new HashSet<Chat>();
                            chats.put(tg, tChat);
                        }
                        tChat.add(e.getChat());
                        
                    }
                    sendResponse(e, "OK - тег(и) "+params[1]+" добавлены к данному чату.");
                }
            }
        });
        
        adminCommands.add(new BaseCommand("\\remove", " [tag1,tag2] - если задан параметр,"
                + " то убрать тег с чата иначе убрать все теги с чата.") {
            @Override
            public void perform(MessageReceivedEvent e, String msg) {
                String[] params = msg.split(" ",2);
                Map<String, Set<Chat>> chats = SkypeHelper.getChats();
                if(params.length==1){
                    for(String key:chats.keySet()){
                        Set<Chat> keyChats = chats.get(key);
                        keyChats.remove(e.getChat());
                    }
                    sendResponse(e, "OK - все теги убраны");
                }else {
                    String[] tags = params[1].split(",");
                    for(String tg:tags){
                        tg=tg.trim();
                        if(tg.isEmpty()) continue;
                        Set<Chat> tChat = chats.get(tg);
                        if(tChat!=null){
                            tChat.remove(e.getChat());
                        }
                    }
                    sendResponse(e, "OK - тег(и) "+params[1]+" убраны");
                }
            }
        });
        
        return adminCommands;
    }

    public static Collection<Command> getUserCommands() {
        if(!commands.isEmpty()) return commands;
        commands.add(new BaseCommand("\\help"," - Список всех команд") {
            
            @Override
            public void perform(MessageReceivedEvent e, String msg) {
                    String resp = "Общедоступные команды: \n";
                    for(Command c:commands){
                        resp+="  "+c.toString()+"\n";
                    }
                    resp+= "Админские команды: \n";
                    for(Command c:adminCommands){
                        resp+="  "+c.toString()+"\n";
                    }
                      sendResponse(e, resp);
            }
           
        });
        commands.add(new BaseCommand("\\unixtime", " [12345678] - вывод текущего"
                + " unixtime или, если задан параметр, перевод из omnitime в unixtime ") {
            @Override
            public void perform(MessageReceivedEvent e, String msg) {
                String[] params = msg.split(" ");
                if(params.length==1){
                    sendResponse(e, "Текущее время(Unix): "+System.currentTimeMillis()/1000);
                }else {
                    try{
                        long omnitime = Long.parseLong(params[1]);
                        Calendar c = new GregorianCalendar(2009, 0,1);
                        c.setTimeZone(TimeZone.getTimeZone("UTC"));
                        sendResponse(e, "Unixtime: "+(omnitime+c.getTimeInMillis()/1000));
                    }catch(Throwable t){
                        sendResponse(e, t.getMessage());
                    }
                }
            }
        });
        commands.add(new BaseCommand("\\omnitime", " [12345678] - вывод текущего"
                + " omnitime или, если задан параметр, перевод из unixtime в omnitime") {
            @Override
            public void perform(MessageReceivedEvent e, String msg) {
                String[] params = msg.split(" ");
                Calendar c = new GregorianCalendar(2009, 0,1);
                c.setTimeZone(TimeZone.getTimeZone("UTC"));
                if(params.length==1){
                    sendResponse(e, "Текущее время(Omni): "+(System.currentTimeMillis()-c.getTimeInMillis())/1000);
                }else {
                    try{
                        long unixtime = Long.parseLong(params[1]);
                        sendResponse(e, "Omnitime: "+(unixtime-c.getTimeInMillis()/1000));
                    }catch(Throwable t){
                        sendResponse(e, t.getMessage());
                    }
                }
            }
        });
        return commands;
    }

    private static List<Command> commands = new ArrayList<Command>();
    private static List<Command> adminCommands = new ArrayList<Command>();

    public static synchronized void sendResponse(MessageReceivedEvent e, String text) {
        try {
            e.getChat().sendMessage(Message.create().with(Text.plain(text)));
        } catch (SkypeException ex) {
            Logger.getLogger(WebSkypeBuilder.class.getName()).log(Level.FATAL, null, ex);
        }
    }
    
}
