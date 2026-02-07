/**
 参考：《基于文本及符号密度的网页正文提取方法》
 参考：https://github.com/GeneralNewsExtractor/GeneralNewsExtractor
 */
(function() {
    'use strict';

    // ==================== 配置项 ====================
    const CONFIG = {
        // 是否在控制台输出调试信息
        verbose: true,

        // 是否自动滚动到正文
        scrollToContent: true,

        // 正文区域最小字符数（低于此值认为是提取失败）
        minContentLength: 200,

        // 正文 CSS 选择器
        // 使用前将 @SELECTOR@ 替换为实际选择器，或替换为空字符串使用自动提取
        contentSelector: '@SELECTOR@' || null,

        // 要删除的 CSS 选择器列表
        noiseSelectors: [
            // 通用噪音
            'script', 'style', 'iframe', 'noscript',
            'nav', 'header', 'footer',
            '.header', '.footer', '.nav', '.navigation',
            '.sidebar', '.side-bar', '.aside',
            '.comment', '.comments', '.comment-list',
            '.related', '.recommend', '.recommendation',
            '.advertisement', '.ad', '.ads', '.banner',
            '.share', '.social', '.social-share',
            '.breadcrumb', '.breadcrumbs',
            '.pagination', '.paging',

            // 国内网站常见噪音
            '.copyright', '.footer-info',
            '.hot-article', '.hot-list',
            '.author-info', '.author-card',
            '.tag', '.tags', '.label',
            '.read-more', '.more-link',

            // 36kr 特定
            '.kr-modal-mask', '.kr-modal-wrapper',
            '.krt-modal', '.krt-toast',
            '.kl-header', '.kl-footer',
            '.stream-item-list',

            // 微信公众号
            '#js_pc_qr_code', '.qr_code_pc',
            '.rich_media_meta', '.profile_container'
        ]
    };

    // ==================== 工具函数 ====================
    function log(msg) {
        if (CONFIG.verbose && console && console.log) {
            console.log('[WebViewExtractor] ' + msg);
        }
    }

    // 计算元素文本密度
    function getTextDensity(element) {
        if (!element) return 0;
        const text = element.textContent || '';
        const textLength = text.trim().length;
        const htmlLength = element.innerHTML.length;
        if (htmlLength === 0) return 0;
        return textLength / htmlLength;
    }

    // 计算元素包含段落标签的数量
    function countParagraphs(element) {
        if (!element) return 0;
        return element.querySelectorAll('p, div, article, section').length;
    }

    // 判断元素是否可能是正文容器
    function isLikelyContent(element) {
        if (!element) return false;

        // 检查常见正文容器的 class/id
        const className = (element.className || '').toLowerCase();
        const id = (element.id || '').toLowerCase();

        const contentHints = [
            'content', 'article', 'post', 'detail', 'main',
            'article-content', 'post-content', 'entry-content',
            'article-body', 'post-body', 'detail-content',
            'rich_media_content', 'article-inner'
        ];

        const hasHint = contentHints.some(hint =>
            className.includes(hint) || id.includes(hint)
        );

        if (hasHint) return true;

        // 检查文本密度和长度
        const density = getTextDensity(element);
        const textLength = (element.textContent || '').trim().length;

        // 密度 > 0.15 且文本长度足够
        return density > 0.15 && textLength > CONFIG.minContentLength;
    }

    // ==================== 核心提取逻辑 ====================

    /**
     * 通过 CSS 选择器查找正文（精确模式）
     */
    function findContentBySelector(selector) {
        if (!selector) return null;

        log('使用选择器查找正文: ' + selector);

        try {
            const element = document.querySelector(selector);
            if (element) {
                const textLength = (element.textContent || '').trim().length;
                log('通过选择器找到正文 (文本: ' + textLength + ' 字符)');
                return element;
            } else {
                log('警告: 选择器未找到任何元素，切换到自动提取模式');
                return null;
            }
        } catch (e) {
            log('选择器无效: ' + e.message + '，切换到自动提取模式');
            return null;
        }
    }
    function findMainContent() {
        log('开始查找正文区域...');

        // 1. 优先尝试查找带有语义化标签的元素
        const semanticCandidates = [
            document.querySelector('article'),
            document.querySelector('[role="main"]'),
            document.querySelector('main'),
            document.querySelector('.article-content'),
            document.querySelector('.post-content'),
            document.querySelector('#article-content'),
            document.querySelector('#post-content')
        ].filter(el => el !== null);

        for (const candidate of semanticCandidates) {
            const textLength = (candidate.textContent || '').trim().length;
            if (textLength > CONFIG.minContentLength) {
                log('通过语义化标签找到正文');
                return candidate;
            }
        }

        // 2. 遍历所有候选元素，找到最可能的正文容器
        const candidates = [];
        const containers = document.querySelectorAll('div, section, article, main');

        containers.forEach(container => {
            const text = (container.textContent || '').trim();
            if (text.length > CONFIG.minContentLength) {
                candidates.push({
                    element: container,
                    textLength: text.length,
                    density: getTextDensity(container),
                    paragraphs: countParagraphs(container)
                });
            }
        });

        if (candidates.length === 0) {
            log('警告：未找到候选正文区域');
            return document.body;
        }

        // 3. 评分：文本长度 * 密度 * 段落数
        candidates.forEach(c => {
            c.score = c.textLength * c.density * (1 + c.paragraphs * 0.1);
        });

        // 4. 排序并返回最高分元素
        candidates.sort((a, b) => b.score - a.score);
        const best = candidates[0];

        log(`找到正文 (分数: ${best.score.toFixed(0)}, 文本: ${best.textLength} 字符)`);
        return best.element;
    }

    // ==================== 图片懒加载修复 ====================
    function fixLazyImages(container) {
        log('修复图片懒加载...');
        const images = container.querySelectorAll('img');
        let fixedCount = 0;

        // 常见懒加载属性
        const lazyAttrs = [
            'data-src', 'data-original', 'data-url', 'data-lazy-src',
            'data-img-src', 'data-real-src', 'data-srcset'
        ];

        images.forEach(img => {
            let realSrc = null;

            // 查找真实图片地址
            for (const attr of lazyAttrs) {
                if (img.hasAttribute(attr)) {
                    realSrc = img.getAttribute(attr);
                    break;
                }
            }

            if (realSrc) {
                img.src = realSrc;
                fixedCount++;

                // 移除干扰属性
                lazyAttrs.forEach(attr => img.removeAttribute(attr));
                if (img.hasAttribute('srcset')) {
                    img.removeAttribute('srcset');
                }
            }

            // 移除可能导致图片不显示的样式
            if (img.style.opacity === '0' || img.style.display === 'none') {
                img.style.opacity = '';
                img.style.display = '';
            }
        });

        log(`修复了 ${fixedCount} 张图片`);
    }

    // ==================== 噪音删除 ====================
    function removeNoise(keepElement) {
        log('删除噪音元素...');

        let removedCount = 0;

        // 1. 删除通过选择器指定的噪音
        CONFIG.noiseSelectors.forEach(selector => {
            try {
                const elements = document.querySelectorAll(selector);
                elements.forEach(el => {
                    // 不要删除要保留的正文本身
                    if (el !== keepElement && !keepElement.contains(el)) {
                        el.remove();
                        removedCount++;
                    }
                });
            } catch (e) {
                // 忽略无效选择器
            }
        });

        // 2. 删除空段落
        const allPs = document.querySelectorAll('p, div');
        allPs.forEach(el => {
            const text = (el.textContent || '').trim();
            const hasImg = el.querySelector('img');
            if (!text && !hasImg) {
                el.remove();
                removedCount++;
            }
        });

        log(`删除了 ${removedCount} 个噪音元素`);
    }

    /**
     * 删除除正文外的所有可见元素（用于指定选择器模式）
     */
    function removeAllExceptContent(contentElement) {
        log('删除正文外的所有元素...');

        let removedCount = 0;

        // 先删除正文内部的噪音元素
        try {
            CONFIG.noiseSelectors.forEach(selector => {
                try {
                    const elements = contentElement.querySelectorAll(selector);
                    elements.forEach(el => {
                        el.remove();
                        removedCount++;
                    });
                } catch (e) {
                    // 忽略无效选择器
                }
            });

            // 删除空段落
            const allPs = contentElement.querySelectorAll('p, div');
            allPs.forEach(el => {
                const text = (el.textContent || '').trim();
                const hasImg = el.querySelector('img');
                if (!text && !hasImg) {
                    el.remove();
                    removedCount++;
                }
            });
        } catch (e) {
            log('清理正文内部元素出错: ' + e.message);
        }

        log(`删除了 ${removedCount} 个元素`);
    }

    // ==================== 样式优化 ====================
    function applyCleanStyle(contentElement) {
        log('应用优化样式...');

        // 清理内容区域的内联样式
        contentElement.style.cssText = '';

        // 为内容区域添加简化的样式
        const style = document.createElement('style');
        style.textContent = `
            body {
                margin: 0 !important;
                padding: 0 !important;
                background: #fff !important;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
                line-height: 1.8 !important;
                color: #333 !important;
                visibility: visible !important;
                text-align: left !important;
            }
            .extracted-content {
                max-width: 800px !important;
                margin: 0 auto !important;
                padding: 20px !important;
                visibility: visible !important;
                display: block !important;
                box-sizing: border-box !important;
                float: none !important;
                text-align: left !important;
            }
            .extracted-content * {
                visibility: visible !important;
            }
            .extracted-content img {
                max-width: 100% !important;
                height: auto !important;
                display: block !important;
                margin: 20px 0 !important;
                border-radius: 4px !important;
            }
            .extracted-content p {
                margin-bottom: 1em !important;
                font-size: 16px !important;
            }
            .extracted-content h1, .extracted-content h2, .extracted-content h3 {
                margin-top: 1.5em !important;
                margin-bottom: 0.8em !important;
                line-height: 1.3 !important;
            }
            .extracted-content blockquote {
                border-left: 4px solid #ddd !important;
                padding-left: 15px !important;
                margin: 20px 0 !important;
                color: #666 !important;
            }
            .extracted-content pre, .extracted-content code {
                background: #f5f5f5 !important;
                padding: 15px !important;
                border-radius: 4px !important;
                overflow-x: auto !important;
            }
        `;
        document.head.appendChild(style);

        // 给内容区域添加标记类
        contentElement.classList.add('extracted-content');
    }

    // ==================== 主函数 ====================
    function extract() {
        log('===== 开始内容提取 =====');

        // 1. 立即隐藏页面，防止闪烁
        if (document.body) {
            document.body.style.visibility = 'hidden';
        }

        try {
            // 2. 查找正文（两种模式）
            let content = null;

            // 模式1: 使用指定的 CSS 选择器
            if (CONFIG.contentSelector) {
                content = findContentBySelector(CONFIG.contentSelector);
            }

            // 模式2: 自动提取（如果没有选择器或选择器失败）
            if (!content) {
                content = findMainContent();
            }

            if (!content) {
                log('错误：未能找到正文内容');
                if (document.body) document.body.style.visibility = 'visible';
                return;
            }

            // 3. 修复图片
            try {
                fixLazyImages(content);
            } catch (e) {
                log('图片修复出错: ' + e.message);
            }

            // 4. 删除噪音（如果有指定选择器，删除更激进）
            try {
                if (CONFIG.contentSelector) {
                    // 指定选择器模式：删除除了正文外的所有可见元素
                    removeAllExceptContent(content);
                } else {
                    // 自动提取模式：只删除噪音元素
                    removeNoise(content);
                }
            } catch (e) {
                log('删除噪音出错: ' + e.message);
            }

            // 5. 清空 body 并只保留正文
            document.body.innerHTML = '';
            document.body.appendChild(content);

            // 6. 应用样式
            try {
                applyCleanStyle(content);
            } catch (e) {
                log('应用样式出错: ' + e.message);
            }

            // 7. 显示页面（确保一定会执行）
            document.body.style.visibility = 'visible';

            // 8. 滚动到顶部
            if (CONFIG.scrollToContent) {
                window.scrollTo(0, 0);
            }

            log('===== 提取完成 =====');
        } catch (e) {
            log('提取过程出错: ' + e.message);
            // 出错也要确保页面可见
            if (document.body) {
                document.body.style.visibility = 'visible';
            }
        }
    }

    // ==================== 手动调用接口 ====================
    // 导出手动调用接口
    window.GNEExtractor = {
        /**
         * 手动执行提取
         * @param {object} options - 可选配置
         * @param {string} options.selector - 指定选择器
         * @param {boolean} options.autoExtract - 是否自动执行（默认 false）
         */
        extract: function(options) {
            options = options || {};

            // 更新配置
            if (options.selector) {
                CONFIG.contentSelector = options.selector;
            }

            // 隐藏页面（在提取前）
            if (document.body) {
                document.body.style.visibility = 'hidden';
            }

            // 延迟执行提取
            setTimeout(function() {
                extract();
            }, 100);
        },

        /**
         * 检查是否已加载
         */
        isReady: function() {
            return true;
        }
    };

    // ==================== 自动执行（向后兼容）====================
    // 默认不自动执行，需要手动调用 GNEExtractor.extract()
    // 如果需要旧版本的行为，设置 CONFIG.autoExtract = true
    if (CONFIG.autoExtract === true) {
        log('自动执行模式已启用');
        if (document.body) {
            document.body.style.visibility = 'hidden';
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', extract);
        } else {
            setTimeout(extract, 100);
        }
    } else {
        log('手动调用模式：使用 GNEExtractor.extract() 启动提取');
    }

})();
