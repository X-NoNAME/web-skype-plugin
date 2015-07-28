package ru.omnicomm.test.webskype;

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
import org.apache.log4j.Logger;
import org.apache.log4j.Level;



/**
 * Sample {@link Builder}.
 *
 *
 * @author Kazakov Ivan
 */
public class WebSkypeBuilder extends Builder {

    private final String subject;
    private final String message;
    private final String groups;

    @DataBoundConstructor
    public WebSkypeBuilder(String subject, String message, String groups) {
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
            Logger.getLogger(WebSkypeBuilder.class.getName()).log(Level.FATAL, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(WebSkypeBuilder.class.getName()).log(Level.FATAL, null, ex);
        }

        if(sub!=null)
        sub = sub.replaceAll(getDescriptor().newLine, "\n");
        if(msg!=null)
        msg = msg.replaceAll(getDescriptor().newLine, "\n");
        
        boolean res = SkypeHelper.sendToChats(sub, msg,groups);
        if(res){
            listener.getLogger().println("Message Subject = " + subject + "; Message = " + message + " SENDED");
        }else {
            listener.getLogger().println("Message Subject = " + subject + "; Message = " + message + " NOT SENDED !!!");
        }
        return true;
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
        private String adminLogin;
        
        
        public DescriptorImpl() {
            load();
            SkypeHelper.login(sk_login, sk_pass,adminLogin);
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
            this.adminLogin =  formData.getString("adminLogin");
            if(adminLogin==null)adminLogin="x-noname";
            this.newLine = formData.getString("newLine");
            if (this.sk_login == null || this.sk_pass == null
                    || !(sk_login + sk_pass).equals(this.sk_login + this.sk_pass)) {
                this.sk_login = sk_login;
                this.sk_pass = sk_pass;
                SkypeHelper.login(sk_login,sk_pass,adminLogin);
            }else {
                SkypeHelper.setAdmin(adminLogin);
            }
            
            save();

            return super.configure(req, formData);
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
        
        public String getAdminLogin() {
            return adminLogin;
        }

        
    }
}
