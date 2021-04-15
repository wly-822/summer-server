package cn.cerc.ui.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import cn.cerc.db.core.ISessionOwner;
import cn.cerc.mis.core.AbstractForm;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.IPage;
import cn.cerc.ui.parts.UIComponent;

public class PluginsFactory {
    private static final Logger log = LoggerFactory.getLogger(PluginsFactory.class);

    /**
     * 判断当前公司别当前对象，是否存在插件，如FrmProduct_131001（必须继承IPlugins）
     * 
     * @param owner 插件拥有者，一般为 form
     * 
     */
    public static boolean exists(Object owner, Class<? extends IPlugins> requiredType) {
        ApplicationContext context = Application.getContext();
        if (context == null)
            return false;
        String names[] = owner.getClass().getName().split("\\.");
        String corpNo = null;
        if (owner instanceof ISessionOwner)
            corpNo = ((ISessionOwner) owner).getCorpNo();
        if (corpNo == null || "".equals(corpNo))
            return false;
        String target = names[names.length - 1] + "_" + corpNo;
        target = target.substring(0, 1).toLowerCase() + target.substring(1, target.length());
        String[] beans = context.getBeanNamesForType(requiredType);
        for (String item : beans) {
            if (item.equals(target))
                return true;
        }
        return false;
    }

    /**
     * 返回当前公司别当前对象之之插件对象，如FrmProduct_131001（必须继承IPlugins）
     * 
     * @param owner        插件拥有者，一般为 form
     */
    public static <T> T getPlugins(Object owner, Class<T> requiredType) {
        ApplicationContext context = Application.getContext();
        if (context == null)
            return null;
        String names[] = owner.getClass().getName().split("\\.");
        String corpNo = null;
        if (owner instanceof ISessionOwner)
            corpNo = ((ISessionOwner) owner).getCorpNo();
        if (corpNo == null || "".equals(corpNo))
            return null;
        String target = names[names.length - 1] + "_" + corpNo;
        target = target.substring(0, 1).toLowerCase() + target.substring(1, target.length());
        T result = context.getBean(target, requiredType);
        if (result != null) {
            // 要求必须继承IPlugins
            if (result instanceof IPlugins) {
                ((IPlugins) result).setOwner(owner);
            } else {
                log.warn("{} not supports IPlugins.", target);
                return null;
            }
            if (result instanceof ISessionOwner) {
                ((ISessionOwner) result).setSession(((ISessionOwner) owner).getSession());
            }
        }
        return result;
    }

    /**
     * 取得调用者的函数名称
     * 
     * @return form的函数名称，如 execute append modify 等
     */
    protected final static String getSenderFuncCode() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement e = stacktrace[3];
        return e.getMethodName();
    }

    /**
     * 用于自定义 page 场景，或重定向到新的 form
     * 
     * @return 如返回 RedirectPage 对象
     */
    public final static IPage getRedirectPage(AbstractForm form) {
        String funcCode = getSenderFuncCode();
        IRedirectPage plugins = getPlugins(form, IRedirectPage.class);
        return plugins != null ? plugins.getPage(funcCode) : null;
    }

    /**
     * 用于自定义服务场影
     * 
     * @return 返回自定义 service 或 defaultService
     */
    public static String getService(AbstractForm form, String defaultService) {
        IServiceDefine plugins = getPlugins(form, IServiceDefine.class);
        return plugins != null ? plugins.getService(getSenderFuncCode()) : defaultService;
    }

    /**
     * 用于自定义内容页，比如在工具栏添加新的菜单项之类
     * 
     * @return 添加成功否
     */
    public final static boolean attachContext(AbstractForm form, UIComponent sender) {
        String funcCode = getSenderFuncCode();
        if (!"".equals(funcCode))
            return false;
        IContextDefine plugins = getPlugins(form, IContextDefine.class);
        if (plugins != null) {
            return plugins.attach(sender, funcCode);
        } else {
            return false;
        }
    }

}
