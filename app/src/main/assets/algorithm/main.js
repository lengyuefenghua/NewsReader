/**
 * ============================================================================
 * WebExtractor - 网页正文提取统一接口
 * ============================================================================
 *
 * @description
 * 提供统一的 JavaScript API 调用三种正文提取算法
 * 支持跨平台消息通知，集成到 WebView 环境
 *
 * @usage
 * // 1. 在页面加载完成后注入此脚本（算法脚本之后）
 * // 2. 调用提取方法
 *
 * @example
 * // CSS 选择器模式
 * WebExtractor.keepOnly({selector: '.post-content'});
 *
 * // 算法提取模式
 * WebExtractor.extract({algorithm: 'readability'});
 *
 * // 提取并通知宿主
 * WebExtractor.extractAndNotify({algorithm: 'readability'});
 *
 * @version 1.0.0
 * ============================================================================
 *
 * ============================================================================
 * Android Kotlin 端集成指南
 * ============================================================================
 *
 * 一、依赖配置
 * ----------
 * 在 build.gradle 中添加 WebView 依赖：
 * ```kotlin
 * implementation "androidx.webkit:webkit:1.8.0"
 * ```
 *
 * 二、布局文件
 * ----------
 * res/layout/activity_main.xml:
 * ```xml
 * <?xml version="1.0" encoding="utf-8"?>
 * <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     android:orientation="vertical">
 *
 *     <WebView
 *         android:id="@+id/webView"
 *         android:layout_width="match_parent"
 *         android:layout_height="0dp"
 *         android:layout_weight="1" />
 *
 *     <Button
 *         android:id="@+id/btnExtract"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="提取正文" />
 * </LinearLayout>
 * ```
 *
 * 三、Kotlin 代码实现
 * -------------------
 * ```kotlin
 * import android.annotation.SuppressLint
 * import android.os.Bundle
 * import android.webkit.WebChromeClient
 * import android.webkit.WebView
 * import android.webkit.WebViewClient
 * import android.widget.Button
 * import androidx.appcompat.app.AppCompatActivity
 * import org.json.JSONObject
 *
 * class MainActivity : AppCompatActivity() {
 *     private lateinit var webView: WebView
 *     private var pageLoadComplete = false
 *
 *     @SuppressLint("SetJavaScriptEnabled")
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.activity_main)
 *
 *         webView = findViewById(R.id.webView)
 *         val btnExtract = findViewById<Button>(R.id.btnExtract)
 *
 *         // 配置 WebView
 *         setupWebView()
 *
 *         // 提取按钮点击事件
 *         btnExtract.setOnClickListener {
 *             extractContent()
 *         }
 *
 *         // 加载测试网页
 *         webView.loadUrl("https://tech.meituan.com/2025/12/19/longcat-interaction-denoiserotator.html")
 *     }
 *
 *     @SuppressLint("SetJavaScriptEnabled")
 *     private fun setupWebView() {
 *         webView.apply {
 *             settings.javaScriptEnabled = true
 *             settings.domStorageEnabled = true
 *
 *             // 设置 WebViewClient 监听页面加载
 *             webViewClient = object : WebViewClient() {
 *                 override fun onPageFinished(view: WebView?, url: String?) {
 *                     super.onPageFinished(view, url)
 *                     pageLoadComplete = true
 *                     injectScripts()
 *                 }
 *             }
 *
 *             // 添加 JavaScript 接口
 *             addJavascriptInterface(WebAppInterface(this), "AndroidInterface")
 *         }
 *     }
 *
 *     private fun injectScripts() {
 *         // 从 assets 目录读取并注入脚本
 *         val scripts = arrayOf(
 *             "Readability-android.js",
 *             "gne-custom.js",
 *             "main.js"
 *         )
 *
 *         Thread {
 *             scripts.forEach { scriptName ->
 *                 val script = assets.open("algorithm/$scriptName").bufferedReader().use { it.readText() }
 *                 runOnUiThread {
 *                     webView.evaluateJavascript(script, null)
 *                 }
 *                 Thread.sleep(100) // 避免注入过快
 *             }
 *         }.start()
 *     }
 *
 *     private fun extractContent() {
 *         if (!pageLoadComplete) {
 *             Toast.makeText(this, "页面尚未加载完成", Toast.LENGTH_SHORT).show()
 *             return
 *         }
 *
 *         // 调用提取方法（这里以 Readability 算法为例）
 *         val command = "WebExtractor.extractAndNotify({algorithm: 'readability', addResponsiveStyle: true});"
 *         webView.evaluateJavascript(command) { result ->
 *             val json = JSONObject(result)
 *             if (json.optBoolean("success")) {
 *                 Toast.makeText(this, "提取成功", Toast.LENGTH_SHORT).show()
 *             } else {
 *                 val error = json.optString("error", "未知错误")
 *                 Toast.makeText(this, "提取失败: $error", Toast.LENGTH_SHORT).show()
 *             }
 *         }
 *     }
 *
 *     // JavaScript 接口类
 *     class WebAppInterface(private val context: AppCompatActivity) {
 *         @JavascriptInterface
 *         fun onPageLoadStatus(status: String, data: String) {
 *             runOnUiThread {
 *                 when (status) {
 *                     "ready" -> {
 *                         // 页面加载完成，可以提取正文
 *                         Toast.makeText(context, "页面就绪", Toast.LENGTH_SHORT).show()
 *                     }
 *                     "waiting" -> {
 *                         // 页面正在加载
 *                     }
 *                     "captcha_required" -> {
 *                         // 需要人机验证
 *                         Toast.makeText(context, "需要完成人机验证", Toast.LENGTH_LONG).show()
 *                     }
 *                     "timeout", "error" -> {
 *                         val json = JSONObject(data)
 *                         val message = json.optString("message", "加载失败")
 *                         Toast.makeText(context, message, Toast.LENGTH_LONG).show()
 *                     }
 *                 }
 *             }
 *         }
 *
 *         @JavascriptInterface
 *         fun onExtractionStatus(status: String, data: String) {
 *             runOnUiThread {
 *                 when (status) {
 *                     "extraction_complete" -> {
 *                         Toast.makeText(context, "提取完成", Toast.LENGTH_SHORT).show()
 *                     }
 *                     "extraction_error" -> {
 *                         val json = JSONObject(data)
 *                         val error = json.optString("error", "未知错误")
 *                         Toast.makeText(context, "提取失败: $error", Toast.LENGTH_LONG).show()
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * 四、文件结构
 * ----------
 * 项目目录结构：
 * ```
 * app/src/main/
 * ├── assets/
 * │   └── algorithm/
 * │       ├── loader.js
 * │       ├── Readability-android.js
 * │       ├── gne-custom.js
 * │       └── main.js
 * ├── java/com/example/webextractor/
 * │   └── MainActivity.kt
 * └── res/
 *     └── layout/
 *         └── activity_main.xml
 * ```
 *
 * 五、完整流程
 * ----------
 * 1. 将所有算法脚本放入 assets/algorithm/ 目录
 * 2. 配置 WebView，启用 JavaScript
 * 3. 添加 JavaScript 接口（AndroidInterface）
 * 4. 加载目标网页
 * 5. 页面加载完成后注入算法脚本
 * 6. 调用 WebExtractor.extractAndNotify() 提取正文
 * 7. 通过 JavaScript 接口接收提取结果
 *
 * 六、消息格式
 * ----------
 * 发送给 Android 的消息格式：
 * {
 *   "type": "page_load_status" | "extraction_status",
 *   "status": "ready" | "waiting" | "captcha_required" | "timeout" | "error" | "extraction_complete" | "extraction_error",
 *   "data": {
 *     "url": string,
 *     "message": string,
 *     "error": string
 *   },
 *   "timestamp": number
 * }
 *
 * 七、注意事项
 * ----------
 * 1. 必须在主线程更新 UI
 * 2. 脚本注入需要在页面加载完成后进行
 * 3. 某些网站可能需要设置 User-Agent
 * 4. 建议使用 Thread 管理脚本注入，避免阻塞主线程
 *
 * ============================================================================
 */

(function() {
    'use strict';

    // 防止重复注入
    if (window.WebExtractor) {
        console.log('WebExtractor already loaded');
        return;
    }

    const WebExtractor = {
        /**
         * 检查算法是否已加载
         * @returns {boolean}
         */
        isReady: function() {
            const ready = typeof CxExtractor !== 'undefined' &&
                         typeof ReadabilityAndroid !== 'undefined';
            console.log('[WebExtractor] Algorithms ready:', ready);
            return ready;
        },

        /**
         * 模式1：CSS 选择器模式
         * @param {object} options
         * @param {string} options.selector - CSS 选择器（必需）
         * @param {boolean} options.keepHead - 是否保留 head，默认 true
         * @param {string} options.action - 'remove' | 'hide'，默认 'remove'
         * @returns {object} {success: boolean, error?: string}
         */
        keepOnly: function(options) {
            console.log('[WebExtractor] keepOnly called with:', options);

            try {
                // 1. 验证参数
                if (!options || !options.selector) {
                    throw new Error('selector 参数是必需的');
                }

                // 2. 查找目标元素
                const target = document.querySelector(options.selector);
                if (!target) {
                    throw new Error(`选择器 "${options.selector}" 未找到任何元素`);
                }

                // 3. 克隆目标元素
                const clone = target.cloneNode(true);

                // 4. 处理 head
                let newHead = '';
                if (options.keepHead !== false) {
                    // 保留 head 中的样式和链接
                    const headElements = document.querySelectorAll(
                        'head style, head link[rel="stylesheet"]'
                    );
                    headElements.forEach(el => {
                        newHead += el.outerHTML;
                    });
                }

                // 5. 替换文档内容
                const newHtml = `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    ${newHead}
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            line-height: 1.6;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            color: #333;
        }
        img {
            max-width: 100%;
            height: auto;
        }
        pre, code {
            background: #f4f4f4;
            padding: 2px 4px;
            border-radius: 3px;
        }
        pre {
            padding: 10px;
            overflow-x: auto;
        }
    </style>
</head>
<body>
    ${clone.outerHTML}
</body>
</html>`;

                document.open();
                document.write(newHtml);
                document.close();

                console.log('[WebExtractor] keepOnly completed successfully');

                // 发送完成通知
                this.notifyHost('extraction_complete', {
                    method: 'keepOnly',
                    selector: options.selector,
                    contentLength: document.body.innerHTML.length
                });

                return { success: true };

            } catch (error) {
                console.error('[WebExtractor] keepOnly error:', error);

                // 发送错误通知
                this.notifyHost('extraction_error', {
                    method: 'keepOnly',
                    selector: options.selector,
                    error: error.message
                });

                return { success: false, error: error.message };
            }
        },

        /**
         * 模式2：算法提取模式
         * @param {object} options
         * @param {string} options.algorithm - 'readability' | 'gne'（必需）
         * @param {boolean} options.addResponsiveStyle - 添加响应式样式，默认 true
         * @returns {object} {success: boolean, error?: string}
         */
        extract: function(options) {
            console.log('[WebExtractor] extract called with:', options);

            try {
                // 1. 验证参数
                if (!options || !options.algorithm) {
                    throw new Error('algorithm 参数是必需的');
                }

                // 2. 根据算法选择
                switch (options.algorithm.toLowerCase()) {
                    case 'readability':
                        return this._extractReadability(options);
                    case 'gne':
                        return this._extractGNE(options);
                    default:
                        throw new Error(`不支持的算法: ${options.algorithm}`);
                }

            } catch (error) {
                console.error('[WebExtractor] extract error:', error);
                return { success: false, error: error.message };
            }
        },

        /**
         * Readability 算法提取
         * @private
         */
        _extractReadability: function(options) {
            console.log('[WebExtractor] Using Readability algorithm');

            if (typeof ReadabilityAndroid === 'undefined') {
                throw new Error('ReadabilityAndroid 未加载');
            }

            // 调用 Readability 解析
            const result = ReadabilityAndroid.parse(document, document);

            if (!result || !result.content) {
                throw new Error('Readability 提取失败');
            }

            // 替换页面内容
            document.body.innerHTML = result.content;

            // 添加响应式样式
            const style = document.createElement('style');
            style.textContent = `
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    line-height: 1.6;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    color: #333;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                pre, code {
                    background: #f4f4f4;
                    padding: 2px 4px;
                    border-radius: 3px;
                }
                pre {
                    padding: 10px;
                    overflow-x: auto;
                }
            `;
            document.head.appendChild(style);

            // 可选：添加元数据
            if (result.title) {
                document.title = result.title;
            }

            console.log('[WebExtractor] Readability completed, content length:', result.content.length);
            return { success: true };
        },

        /**
         * GNE 算法提取
         * @private
         */
        _extractGNE: function(options) {
            console.log('[WebExtractor] Using GNE algorithm');

            // GNE 需要特殊处理，因为它原本是为自动执行设计的
            if (typeof GNEExtractor !== 'undefined' && GNEExtractor.extract) {
                // 如果 GNE 已被改造为可手动调用
                GNEExtractor.extract({
                    selector: options.selector,
                    autoExtract: false
                });
                return { success: true };
            }

            // 如果 GNE 还是自动执行版本，需要重新实现提取逻辑
            // 这里简化处理，建议使用其他算法
            throw new Error('GNE 算法需要改造后才能使用，请使用 cx 或 readability');
        },

        /**
         * 提取并通知宿主
         * @param {object} options - 提取选项
         * @returns {object} 提取结果
         */
        extractAndNotify: function(options) {
            console.log('[WebExtractor] extractAndNotify called');

            try {
                const result = this.extract(options);

                if (result.success) {
                    this.notifyHost('extraction_complete', {
                        algorithm: options.algorithm,
                        contentLength: document.body.innerHTML.length,
                        url: window.location.href
                    });
                } else {
                    this.notifyHost('extraction_error', {
                        algorithm: options.algorithm,
                        error: result.error
                    });
                }

                return result;

            } catch (error) {
                console.error('[WebExtractor] extractAndNotify error:', error);
                this.notifyHost('extraction_error', {
                    algorithm: options.algorithm,
                    error: error.message
                });
                throw error;
            }
        },

        /**
         * 通知宿主应用（跨平台兼容）
         * @param {string} status - 状态码
         * @param {object} data - 附加数据
         */
        notifyHost: function(status, data) {
            const message = {
                type: 'extraction_status',
                status: status,
                data: data,
                timestamp: Date.now()
            };

            console.log('[WebExtractor] Notify host:', status, data);

            // 方式1: WebView2 (C#)
            if (window.chrome && window.chrome.webview) {
                window.chrome.webview.postMessage(message);
                return;
            }

            // 方式2: Android (Kotlin/Java)
            if (window.AndroidInterface) {
                window.AndroidInterface.onExtractionStatus(status, JSON.stringify(data));
                return;
            }

            // 方式3: iOS (Swift)
            if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.extractionHandler) {
                window.webkit.messageHandlers.extractionHandler.postMessage(message);
                return;
            }

            // 方式4: 通用 - 设置 window 属性
            window.__extractionStatus = message;
        }
    };

    // 暴露到全局
    window.WebExtractor = WebExtractor;

    console.log('[WebExtractor] Loaded successfully');
    console.log('[WebExtractor] Ready:', WebExtractor.isReady());
})();
