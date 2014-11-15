package net.oschina.app.ui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.oschina.app.AppContext;
import net.oschina.app.AppException;
import net.oschina.app.R;
import net.oschina.app.api.ApiClient;
import net.oschina.app.bean.Result;
import net.oschina.app.bean.URLs;
import net.oschina.app.bean.User;
import net.oschina.app.common.StringUtils;
import net.oschina.app.common.UIHelper;
import net.wolfcs.network.LoginRequest;
import net.wolfcs.network.RequestManager;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ViewSwitcher;

import com.android.volley.Response;
import com.android.volley.VolleyError;

/**
 * 用户登录对话框
 * 
 * @author liux (http://my.oschina.net/liux)
 * @version 1.0
 * @created 2012-3-21
 */
public class LoginDialog extends BaseActivity {

    private ViewSwitcher mViewSwitcher;
    private ImageButton btn_close;
    private Button btn_login;
    private AutoCompleteTextView mAccount;
    private EditText mPwd;
    private AnimationDrawable loadingAnimation;
    private View loginLoading;
    private CheckBox chb_rememberMe;
    private int curLoginType;
    private InputMethodManager imm;

    public final static int LOGIN_OTHER = 0x00;
    public final static int LOGIN_MAIN = 0x01;
    public final static int LOGIN_SETTING = 0x02;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_dialog);

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        curLoginType = getIntent().getIntExtra("LOGINTYPE", LOGIN_OTHER);

        mViewSwitcher = (ViewSwitcher) findViewById(R.id.logindialog_view_switcher);
        loginLoading = (View) findViewById(R.id.login_loading);
        mAccount = (AutoCompleteTextView) findViewById(R.id.login_account);
        mPwd = (EditText) findViewById(R.id.login_password);
        chb_rememberMe = (CheckBox) findViewById(R.id.login_checkbox_rememberMe);

        btn_close = (ImageButton) findViewById(R.id.login_close_button);
        btn_close.setOnClickListener(UIHelper.finish(this));

        btn_login = (Button) findViewById(R.id.login_btn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 隐藏软键盘
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                String account = mAccount.getText().toString();
                String pwd = mPwd.getText().toString();
                boolean isRememberMe = chb_rememberMe.isChecked();
                // 判断输入
                if (StringUtils.isEmpty(account)) {
                    UIHelper.ToastMessage(v.getContext(), getString(R.string.msg_login_email_null));
                    return;
                }
                if (StringUtils.isEmpty(pwd)) {
                    UIHelper.ToastMessage(v.getContext(), getString(R.string.msg_login_pwd_null));
                    return;
                }

                btn_close.setVisibility(View.GONE);
                loadingAnimation = (AnimationDrawable) loginLoading.getBackground();
                loadingAnimation.start();
                mViewSwitcher.showNext();

                login(account, pwd, isRememberMe);
            }
        });

        // 是否显示登录信息
        AppContext ac = (AppContext) getApplication();
        User user = ac.getLoginInfo();
        if (user == null || !user.isRememberMe())
            return;
        if (!StringUtils.isEmpty(user.getAccount())) {
            mAccount.setText(user.getAccount());
            mAccount.selectAll();
            chb_rememberMe.setChecked(user.isRememberMe());
        }
        if (!StringUtils.isEmpty(user.getPwd())) {
            mPwd.setText(user.getPwd());
        }
    }

    private class LoginResponseListener implements Response.Listener<String>, Response.ErrorListener {
        private final AppContext mAppContext;
        private final String mAccount;
        private final String mPassword;
        private final boolean mIsRememberMe;

        public LoginResponseListener(AppContext ac, String account, String password, boolean isRememberMe) {
            mAppContext = ac;
            mAccount = account;
            mPassword = password;
            mIsRememberMe = isRememberMe;
        }

        @Override
        public void onErrorResponse(VolleyError error) {

        }

        @Override
        public void onResponse(String response) {
            InputStream in_nocode = new ByteArrayInputStream(response.getBytes());
            try {
                User user = User.parse(in_nocode);
                user.setAccount(mAccount);
                user.setPwd(mPassword);
                user.setRememberMe(mIsRememberMe);
                Result res = user.getValidate();
                if (res.OK()) {
                    mAppContext.saveLoginInfo(user);// 保存登录信息
                    if (user != null) {
                        // 清空原先cookie
                        ApiClient.cleanCookie();
                        // 发送通知广播
                        UIHelper.sendBroadCast(LoginDialog.this, user.getNotice());
                        // 提示登陆成功
                        UIHelper.ToastMessage(LoginDialog.this, R.string.msg_login_success);
                        if (curLoginType == LOGIN_MAIN) {
                            // 跳转--加载用户动态
                            Intent intent = new Intent(LoginDialog.this, Main.class);
                            intent.putExtra("LOGIN", true);
                            startActivity(intent);
                        } else if (curLoginType == LOGIN_SETTING) {
                            // 跳转--用户设置页面
                            Intent intent = new Intent(LoginDialog.this, Setting.class);
                            intent.putExtra("LOGIN", true);
                            startActivity(intent);
                        }
                        finish();
                    }
                } else {
                    mAppContext.cleanLoginInfo();// 清除登录信息
                    mViewSwitcher.showPrevious();
                    btn_close.setVisibility(View.VISIBLE);
                    UIHelper.ToastMessage(LoginDialog.this, getString(R.string.msg_login_fail) + user);
                }
                return;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AppException e) {
                e.printStackTrace();
                mViewSwitcher.showPrevious();
                btn_close.setVisibility(View.VISIBLE);
                e.makeToast(LoginDialog.this);
            }

        }
    }

    // 登录验证
    private void login(String account, String pwd, boolean isRememberMe) {
        AppContext ac = (AppContext) getApplication();
        LoginResponseListener responseListener = new LoginResponseListener(ac, account, pwd, isRememberMe);
        String loginurl = URLs.LOGIN_VALIDATE_HTTP;

        if (ac.isHttpsLogin()) {
            loginurl = URLs.LOGIN_VALIDATE_HTTPS;
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", account);
        params.put("pwd", pwd);
        params.put("keep_login", String.valueOf(1));
        AppContext appContext = (AppContext) getApplication();
        LoginRequest loginRequest = new LoginRequest(loginurl, params, appContext, responseListener, responseListener);

        RequestManager.getRequestManager(getApplicationContext()).executeRequest(loginRequest);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.onDestroy();
        }
        return super.onKeyDown(keyCode, event);
    }
}
