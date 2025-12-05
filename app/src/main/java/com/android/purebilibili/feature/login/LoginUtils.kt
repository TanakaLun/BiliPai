package com.android.purebilibili.feature.login

// 注入到 WebView 的 JS/CSS，用于隐藏不必要的元素
const val WEB_LOGIN_INJECT_JS = """
    javascript:(function() {
        var style = document.createElement('style');
        style.innerHTML = `
            /* 隐藏顶部导航、Logo条、底部Footer、下载提示 */
            .m-navbar, .biligame-navbar, header, .header, 
            .global-header, .nav-bar, .bili-header,
            .footer, .bili-footer, .global-footer, .m-footer,
            .bottom-banner, .launch-app-btn, .app-download-guide,
            .right-side-bar, .top-banner,
            div[class*="header"], div[class*="nav"],
            div[class*="footer"] { 
                display: none !important; 
            }

            /* 隐藏 WebView 内部的二维码登录区域 (App已有原生扫码) */
            .qrcode-login, .scan-box, .qrcode-con, .qr-code-box,
            div[class*="qrcode"], div[class*="scan"],
            .login-scan-box {
                display: none !important;
            }
            
            /* 隐藏装饰性插画 */
            img[src*="22_33"], .left-img, .right-img, .bottom-img,
            .login-left-img, .login-right-img {
                display: none !important;
            }

            /* 基础布局重置 */
            html {
                width: 100% !important;
                min-height: 100% !important;
                margin: 0 !important;
                padding: 0 !important;
                /* 允许滚动，但隐藏水平滚动条 */
                overflow-x: hidden !important;
                overflow-y: auto !important;
            }

            body { 
                background-color: #fff !important; 
                width: 100% !important;
                min-height: 100vh !important; /* 确保占满屏幕 */
                margin: 0 !important;
                padding: 0 !important;
                
                display: flex !important;
                flex-direction: column !important;
                align-items: center !important;
                /* 使用 padding 代替 justify-content: center，防止键盘弹出时内容被挤出视口顶部 */
                padding-top: 40px !important; 
                padding-bottom: 40px !important;
                box-sizing: border-box !important;
            }

            /* 针对 PC/宽屏版容器的强制调整 */
            .login-box, .main-content, #login-app, .box-content, .card {
                width: 90% !important; /* 移动端留出边距 */
                max-width: 400px !important; /* 限制最大宽度 */
                margin: 0 auto !important;
                box-shadow: none !important;
                transform: none !important;
                position: relative !important;
                left: auto !important;
                top: auto !important;
                background: transparent !important;
            }
            
            /* 强制显示表单区域 */
            .form-login, .login-form, .sms-login {
                display: block !important;
                width: 100% !important;
            }
        `;
        document.head.appendChild(style);
    })()
"""
