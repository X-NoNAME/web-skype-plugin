package ru.omnicomm.test.webskype;

import com.samczsun.skype4j.Skype;
import com.samczsun.skype4j.chat.Chat;
import com.samczsun.skype4j.events.EventHandler;
import com.samczsun.skype4j.events.Listener;
import com.samczsun.skype4j.events.chat.message.MessageReceivedEvent;
import com.samczsun.skype4j.exceptions.SkypeException;
import com.samczsun.skype4j.formatting.Message;
import com.samczsun.skype4j.formatting.Text;
import hudson.Launcher;
import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample {@link WebSkypePlugin}.
 *
 *
 *
 * @author X-NoNAME
 */
public class WebSkypePlugin extends Builder {

    private final String subject;
    private final String message;
    private final String groups;

    @DataBoundConstructor
    public WebSkypePlugin(String subject, String message, String groups) {
        this.subject = subject;
        this.message = message;
        this.groups = groups;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public String getGroups() {
        return groups;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String msg = message;
        String sub = subject;
        try {
            msg = Util.replaceMacro(Util.replaceMacro(message, build.getBuildVariableResolver()), build.getEnvironment(listener));
            sub = Util.replaceMacro(Util.replaceMacro(subject, build.getBuildVariableResolver()), build.getEnvironment(listener));
        } catch (IOException ex) {
            Logger.getLogger(WebSkypePlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(WebSkypePlugin.class.getName()).log(Level.SEVERE, null, ex);
        }

        sub = sub.replaceAll(getDescriptor().newLine, "\n");
        msg = msg.replaceAll(getDescriptor().newLine, "\n");
        if (groups.isEmpty()) {
            if (getDescriptor().chats.get("default") == null) {
                return true;
            } else {
                sendToChats("default", listener, sub, msg);
            }
        } else {
            for (String gr : groups.split(",")) {
                if (getDescriptor().chats.get(gr) == null) {
                    continue;
                }
                sendToChats(gr, listener, sub, msg);
            }
        }
        return true;
    }

    public void sendToChats(String gr, BuildListener listener, String subject, String message) {
        for (Chat ch : getDescriptor().chats.get(gr)) {
            try {
                ch.sendMessage(Message.fromHtml("<b>" + subject + "</b>\n" + message));
                listener.getLogger().println("Message Subject = " + subject + "; Message = " + message + " sended");
            } catch (SkypeException ex) {
                Logger.getLogger(WebSkypePlugin.class.getName()).log(Level.SEVERE, null, ex);
                listener.getLogger().println("Message Subject = " + subject + "; Message = " + message + " not sended");
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String sk_login;
        private String sk_pass;
        private String newLine;
        private transient Map<String, Set<Chat>> chats = new HashMap<String, Set<Chat>>();
        private transient Skype skype;

        public DescriptorImpl() {
            load();
            loadSkype();
        }

        public FormValidation doCheckSubject(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a Subject");
            }
            if (value.length() < 4) {
                return FormValidation.warning("Isn't the Subject too short?");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckGroups(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a Subject");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Web-skype send message";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            String sk_login = formData.getString("sk_login");
            String sk_pass = formData.getString("sk_pass");
            if (this.sk_login == null || this.sk_pass == null
                    || !(sk_login + sk_pass).equals(this.sk_login + this.sk_pass)) {
                this.sk_login = sk_login;
                this.sk_pass = sk_pass;
                loadSkype();
            }
            this.newLine = formData.getString("newLine");
            save();

            return super.configure(req, formData);
        }

        public void sendResponse(MessageReceivedEvent e, String text) {
            try {
                e.getChat().sendMessage(Message.create().with(Text.plain(text)));
            } catch (SkypeException ex) {
                Logger.getLogger(WebSkypePlugin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public String getSk_login() {
            return sk_login;
        }

        public String getSk_pass() {
            return sk_pass;
        }

        public String getNewLine() {
            return newLine;
        }

        private void loadSkype() {

            if (skype != null) {
                try {
                    skype.logout();
                } catch (IOException ex) {
                    Logger.getLogger(WebSkypePlugin.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (sk_login == null || sk_pass == null || sk_login.isEmpty() || sk_pass.isEmpty()) {
                return;
            }

            try {
                skype = Skype.login(sk_login, sk_pass);
                skype.getEventDispatcher().registerListener(new Listener() {

                    @EventHandler
                    public void onMessage(MessageReceivedEvent e) {
                        String msg = e.getMessage().getMessage().asPlaintext();
                        if (true /*e.getMessage().getSender().getUsername().equals("x-noname")*/) {

                            if (msg.startsWith("\\tagadd")) {
                                String[] s = msg.split(" ");
                                String group = s.length > 1 ? s[1].trim() : "default";
                                addChatToGroup(e.getChat(), group);
                                sendResponse(e, "OK - tag " + group + " added");
                            }
                            if (msg.startsWith("\\tagremove")) {
                                String[] s = msg.split(" ");
                                String group = s.length > 1 ? s[1].trim() : "default";
                                removeChatFromGroup(e.getChat(), group);
                                sendResponse(e, "OK - tag " + group + " removed");
                            }
                            if (msg.equals("\\tagsall")) {
                                String s = "";
                                for (String key : chats.keySet()) {
                                    s += "tag: " + key + ", chats: " + chats.get(key).size() + "\n";
                                }
                                sendResponse(e, s.isEmpty() ? "Empty list" : s);
                            }
                            if (msg.equals("\\tags")) {
                                String s = "";
                                for (String key : chats.keySet()) {
                                    for (Chat ch : chats.get(key)) {
                                        if (e.getChat().equals(ch)) {
                                            s += key + ", ";
                                        }
                                    }
                                }
                                sendResponse(e, s.isEmpty() ? "no tags" : s);
                            }
                            if (msg.equals("\\tagsclear")) {
                                for (String key : chats.keySet()) {
                                    chats.get(key).remove(e.getChat());
                                }
                                sendResponse(e, "OK - all tags removed from this chat");
                            }
                            if (msg.equals("\\help")) {

                                sendResponse(e, "\\tagadd tag - add tag to this chat\n"
                                        + "\\tagremove tag - remove tag from this chat\n"
                                        + "\\tagsall - list of all tags\n"
                                        + "\\tags - list of tags for current chat\n"
                                        + "\\tagsclear - remove all tags from this chat\n"
                                        + "\\help - this help");
                            }
                        }
                    }

                    private void addChatToGroup(Chat chat, String group) {
                        Set<Chat> set = chats.get(group);
                        if (set == null) {
                            set = new HashSet<Chat>();
                            chats.put(group, set);
                        }
                        set.add(chat);
                    }

                    private void removeChatFromGroup(Chat chat, String group) {
                        Set<Chat> set = chats.get(group);
                        if (set != null) {
                            set.remove(chat);
                        }
                    }

                });
                skype.subscribe();
            } catch (SkypeException ex) {
                Logger.getLogger(WebSkypePlugin.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(WebSkypePlugin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
