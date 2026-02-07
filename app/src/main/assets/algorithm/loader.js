/**
 * ============================================================================
 * PageLoader - 页面加载状态检测脚本
 * ============================================================================
 *
 * @description
 * 在页面导航前注入，监听页面加载状态，识别特殊场景（重定向、人机验证等）
 * 通过跨平台消息通知宿主应用
 *
 * @usage
 * 1. 在导航前注入此脚本
 * 2. 调用 PageLoader.start(options) 启动监听
 * 3. 监听 WebMessage 消息接收状态更新
 *
 * @example
 * // C# 端
 * await webView.CoreWebView2.ExecuteScriptAsync(File.ReadAllText("algorithm/loader.js"));
 * await webView.CoreWebView2.ExecuteScriptAsync("PageLoader.start({timeout: 30000})");
 * webView.CoreWebView2.WebMessageReceived += OnWebMessageReceived;
 *
 * @message
 * 发送给宿主的消息格式：
 * {
 *   type: 'page_load_status',
 *   status: 'ready' | 'redirecting' | 'waiting' | 'captcha_required' | 'timeout' | 'error',
 *   data: { url: string, message?: string, reason?: string, loadTime?: number },
 *   timestamp: number
 * }
 *
 * @version 1.0.0
 * ============================================================================
 */

(function() {
    'use strict';

    // 防止重复注入
    if (window.PageLoader) {
        console.log('PageLoader already loaded');
        return;
    }

    const PageLoader = {
        // 配置参数
        config: {
            timeout: 30000,              // 默认超时 30 秒
            checkInterval: 100,          // 检查间隔 100ms
            redirectLimit: 10,           // 最大重定向次数

            // "请等待"模式关键词（正则表达式列表）
            waitPatterns: [
                /please\s+wait/i,
                /加载中/i,
                /loading/i,
                /正在加载/i,
                /请稍候/i,
                /请稍等/i,
                /wait\s+a\s+moment/i
            ],

            // 人机验证模式关键词
            captchaPatterns: [
                // 移除了 /captcha/i，因为误报率太高：
                // - 阿里云验证码SDK配置对象: window.aliyunCaptchaConfig
                // - 网站登录/注册提示文字: "需要验证码"
                // - 其他非验证码页面的提及
                // 改用更精确的UI提示语：
                /please\s+complete\s+the\s+security\s+check/i,
                /please\s+verify\s+you\s+are\s+human/i,
                /complete\s+the\s+verification/i,
                /人机身份验证/i,
                /请完成人机验证/i,
                /请点击下方按钮完成验证/i,
                /请拖动滑块/i,
                /slide\s+to\s+verify/i,
                /recaptcha/i,
                /hcaptcha/i,
                /distil\s+networks/i,
                /access\s+denied/i,
                /just\s+a\s+moment/i,
                /checking\s+your\s+browser/i
            ]
        },

        // 状态变量
        state: {
            redirectCount: 0,
            lastUrl: window.location.href,
            timer: null,
            startTime: Date.now(),
            isRunning: false
        },

        /**
         * 检查是否是人机验证页面
         * @returns {boolean}
         */
        isCaptchaPage: function() {
            if (!document.body) return false;

            const bodyText = document.body.textContent.toLowerCase();
            const title = (document.title || '').toLowerCase();
            const url = window.location.href.toLowerCase();

            // 1. 检查页面文本和标题
            for (let pattern of this.config.captchaPatterns) {
                if (pattern.test(bodyText) || pattern.test(title) || pattern.test(url)) {
                    const matchIn = pattern.test(bodyText) ? 'bodyText' : (pattern.test(title) ? 'title' : 'url');

                    // 找出匹配的具体文本片段
                    let contextText = '';
                    if (matchIn === 'bodyText') {
                        const match = bodyText.match(pattern);
                        if (match) {
                            const idx = bodyText.indexOf(match[0]);
                            const start = Math.max(0, idx - 30);
                            const end = Math.min(bodyText.length, idx + match[0].length + 30);
                            contextText = ` "...${bodyText.substring(start, end)}..."`;
                        }
                    }

                    const reason = `Pattern: ${pattern.toString()} matched in ${matchIn}${contextText}`;
                    console.error('[PageLoader] Captcha detected -', reason);
                    this._lastCaptchaReason = reason;  // 保存原因
                    return true;
                }
            }

            // 2. 检查特定的验证 iframe
            const captchaFrames = document.querySelectorAll(
                'iframe[src*="recaptcha"], ' +
                'iframe[src*="hcaptcha"], ' +
                'iframe[src*="challenges.cloudflare"], ' +
                'iframe[src*="api.cloudflare"]'
            );
            if (captchaFrames.length > 0) {
                const reason = `Found ${captchaFrames.length} captcha iframe(s)`;
                console.error('[PageLoader] Captcha detected -', reason);
                this._lastCaptchaReason = reason;
                return true;
            }

            // 3. 检查 Cloudflare 特征
            const cloudflareElements = document.querySelectorAll(
                '[data-ray], ' +
                'div[id*="challenge"], ' +
                'form[action*="challenge"]'
            );
            if (cloudflareElements.length > 0) {
                const elementInfos = [];
                cloudflareElements.forEach((el, i) => {
                    elementInfos.push(`${el.tagName}${el.id ? '#' + el.id : ''}${el.className ? '.' + el.className : ''}`);
                });
                const reason = `Found ${cloudflareElements.length} Cloudflare elements: [${elementInfos.join(', ')}]`;
                console.error('[PageLoader] Captcha detected -', reason);
                this._lastCaptchaReason = reason;
                return true;
            }

            return false;
        },

        /**
         * 检查是否是"请等待"页面
         * @returns {boolean}
         */
        isWaitingPage: function() {
            if (!document.body) return false;

            const bodyText = document.body.textContent.toLowerCase();
            const title = (document.title || '').toLowerCase();

            for (let pattern of this.config.waitPatterns) {
                if (pattern.test(bodyText) || pattern.test(title)) {
                    console.log('[PageLoader] Waiting page detected:', pattern);
                    return true;
                }
            }
            return false;
        },

        /**
         * 检查页面是否有实质内容
         * @returns {boolean}
         */
        hasContent: function() {
            if (!document.body) return false;

            // 1. 检查正文长度
            const textLength = document.body.textContent.trim().length;
            if (textLength < 100) {
                console.log('[PageLoader] Content too short:', textLength);
                return false;
            }

            // 2. 检查是否有常见的正文元素
            const hasParagraphs = document.querySelectorAll('body p').length > 0;
            const hasArticle = document.querySelectorAll(
                'body article, ' +
                'body .content, ' +
                'body .post, ' +
                'body .article, ' +
                'body .post-content, ' +
                'body .main-content, ' +
                'body .entry-content'
            ).length > 0;

            const hasContent = hasParagraphs || hasArticle;
            console.log('[PageLoader] Has content:', hasContent, '(paragraphs:', hasParagraphs, ', article:', hasArticle, ')');
            return hasContent;
        },

        /**
         * 检查是否发生了重定向
         * @returns {string|null} 'redirecting' | 'redirect_limit' | null
         */
        checkRedirect: function() {
            const currentUrl = window.location.href;

            if (currentUrl !== this.state.lastUrl) {
                this.state.redirectCount++;
                this.state.lastUrl = currentUrl;
                console.log('[PageLoader] Redirect detected:', currentUrl, 'count:', this.state.redirectCount);

                if (this.state.redirectCount > this.config.redirectLimit) {
                    console.log('[PageLoader] Redirect limit exceeded');
                    return 'redirect_limit';
                }
                return 'redirecting';
            }
            return null;
        },

        /**
         * 通知宿主应用（跨平台兼容）
         * @param {string} status - 状态码
         * @param {object} data - 附加数据
         */
        notifyHost: function(status, data) {
            const message = {
                type: 'page_load_status',
                status: status,
                data: data,
                timestamp: Date.now()
            };

            console.log('[PageLoader] Notify host:', status, data);

            // 方式1: WebView2 (C#)
            if (window.chrome && window.chrome.webview) {
                window.chrome.webview.postMessage(message);
                return;
            }

            // 方式2: Android (Kotlin/Java)
            if (window.AndroidInterface) {
                window.AndroidInterface.onPageLoadStatus(status, JSON.stringify(data));
                return;
            }

            // 方式3: iOS (Swift)
            if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.pageLoadHandler) {
                window.webkit.messageHandlers.pageLoadHandler.postMessage(message);
                return;
            }

            // 方式4: 通用 - 设置 window 属性（轮询检测）
            window.__pageLoadStatus = message;
        },

        /**
         * 启动监听
         * @param {object} options - 配置选项
         */
        start: function(options) {
            if (this.state.isRunning) {
                console.warn('[PageLoader] Already running');
                return;
            }

            if (options) {
                Object.assign(this.config, options);
            }

            this.state.startTime = Date.now();
            this.state.isRunning = true;
            this.state.redirectCount = 0;
            this.state.lastUrl = window.location.href;

            console.log('[PageLoader] Started with config:', this.config);

            this.state.timer = setInterval(() => {
                this.check();
            }, this.config.checkInterval);
        },

        /**
         * 停止监听
         */
        stop: function() {
            if (this.state.timer) {
                clearInterval(this.state.timer);
                this.state.timer = null;
            }
            this.state.isRunning = false;
            console.log('[PageLoader] Stopped');
        },

        /**
         * 主检查逻辑
         */
        check: function() {
            // 1. 检查超时
            if (Date.now() - this.state.startTime > this.config.timeout) {
                this.stop();
                this.notifyHost('timeout', {
                    url: window.location.href,
                    message: '页面加载超时'
                });
                return;
            }

            // 2. 检查重定向
            const redirectStatus = this.checkRedirect();
            if (redirectStatus === 'redirect_limit') {
                this.stop();
                this.notifyHost('error', {
                    reason: 'too_many_redirects',
                    url: window.location.href,
                    message: '重定向次数过多'
                });
                return;
            }
            if (redirectStatus === 'redirecting') {
                this.notifyHost('redirecting', {
                    url: window.location.href,
                    redirectCount: this.state.redirectCount
                });
                return;
            }

            // 3. 检查页面状态
            const readyState = document.readyState;
            if (readyState !== 'complete') {
                return; // 还在加载中
            }

            // 4. 加载完成，检查特殊情况
            if (this.isCaptchaPage()) {
                this.stop();
                this.notifyHost('captcha_required', {
                    url: window.location.href,
                    message: '需要人机验证',
                    reason: this._lastCaptchaReason || 'Unknown reason'
                });
                return;
            }

            if (this.isWaitingPage()) {
                this.notifyHost('waiting', {
                    url: window.location.href,
                    message: '页面正在加载，请稍候'
                });
                return;
            }

            // 5. 检查是否有实质内容
            if (this.hasContent()) {
                this.stop();
                this.notifyHost('ready', {
                    url: window.location.href,
                    loadTime: Date.now() - this.state.startTime
                });
                return;
            }
        }
    };

    // 暴露到全局
    window.PageLoader = PageLoader;

    console.log('[PageLoader] Loaded successfully');
})();
