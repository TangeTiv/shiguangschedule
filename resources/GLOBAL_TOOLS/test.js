/**
 * 华南师范大学（正方教务系统）成绩抓取测试脚本 - 接口直连版
 *
 * 运行环境：Android WebView（已登录状态，Session/Cookie 自动携带）
 * 功能：直接请求成绩查询接口，解析 JSON 并返回成绩列表
 * 输出：alert 弹出 JSON，并通过 AndroidBridge.showToast 提示数量
 */

(function () {
    "use strict";

    /**
     * 收集查询参数：序列化 searchForm +
     * 补充表单之外的隐藏字段（pageNumber 控制每页条数）
     */
    function getQueryParams() {
        var form = document.getElementById("searchForm");
        if (!form) {
            AndroidBridge.showToast("未找到 searchForm，请确认页面是否完整加载");
            return null;
        }

        var params = new URLSearchParams();

        // 序列化表单全部字段（含 hidden input）
        if (typeof FormData !== "undefined") {
            var formData = new FormData(form);
            formData.forEach(function (value, key) {
                params.append(key, value);
            });
        } else {
            var elements = form.elements;
            for (var i = 0; i < elements.length; i++) {
                var el = elements[i];
                if (el.name && el.name !== "" && el.type !== "submit") {
                    params.append(el.name, el.value);
                }
            }
        }

        // pageNumber 在 <form> 之外，需单独读取
        var pageNumberEl = document.getElementById("pageNumber");
        if (pageNumberEl && pageNumberEl.value) {
            params.set("pageNumber", pageNumberEl.value);
            params.set("rows", pageNumberEl.value);
        } else {
            params.set("rows", "15");
        }

        return params;
    }

    /**
     * 解析 items 数组中的单条记录为统一格式
     */
    function extractItem(item) {
        return {
            courseName: (item.kcmc || "").trim(),
            credit: (item.xf || "").toString().trim(),
            score: (item.cj || "").toString().trim(),
            gpa: (item.jd || "").toString().trim()
        };
    }

    /**
     * 解析接口返回的 JSON 数据，提取成绩列表
     * @param {Object} json - 接口返回的 JSON 对象
     * @returns {Array} 成绩数组，或 null（格式未知）
     */
    function parseResponseJson(json) {
        // 方式1：{ rows: [{ cell: [...] }] } jqGrid 标准格式
        if (json.rows && Array.isArray(json.rows) && json.rows.length > 0) {
            var results = [];
            for (var i = 0; i < json.rows.length; i++) {
                var cells = json.rows[i].cell;
                if (cells && cells.length >= 4) {
                    results.push({
                        courseName: cells[0] ? cells[0].trim() : "",
                        credit: cells[1] ? cells[1].trim() : "",
                        score: cells[2] ? cells[2].trim() : "",
                        gpa: cells[3] ? cells[3].trim() : ""
                    });
                }
            }
            return results;
        }

        // 方式2：直返数组
        if (Array.isArray(json)) {
            return json.map(function (item) {
                return {
                    courseName: (item.kcmc || item.courseName || "").trim(),
                    credit: (item.xf || item.credit || "").toString().trim(),
                    score: (item.cj || item.score || "").toString().trim(),
                    gpa: (item.jd || item.gpa || "").toString().trim()
                };
            });
        }

        return null;
    }

    /**
     * 主入口：获取成绩数据（单次请求，不依赖分页）
     */
    function fetchScores() {
        console.log("成绩抓取脚本开始执行...");

        var params = getQueryParams();
        if (!params) return;

        console.log("请求参数:", params.toString());

        fetch("/cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest"
            },
            body: params.toString(),
            credentials: "include"
        })
        .then(function (response) {
            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }
            return response.json();
        })
        .then(function (data) {
            var rawItems = data.items;
            if (!rawItems || !Array.isArray(rawItems) || rawItems.length === 0) {
                // items 不存在则尝试其他格式
                var parsed = parseResponseJson(data);
                if (parsed && parsed.length > 0) {
                    alert(JSON.stringify(parsed, null, 2));
                    AndroidBridge.showToast("共抓取到 " + parsed.length + " 条成绩数据");
                    return;
                }
                AndroidBridge.showToast("未获取到成绩数据");
                alert("无法解析成绩，原始响应：\n" + JSON.stringify(data, null, 2));
                return;
            }

            var results = rawItems.map(extractItem);
            var output = JSON.stringify(results, null, 2);

            console.log("成功抓取 " + results.length + " 条成绩");
            AndroidBridge.showToast("共抓取到 " + results.length + " 条成绩数据");
            alert(output);
        })
        .catch(function (error) {
            console.error("请求失败:", error);
            AndroidBridge.showToast("请求成绩接口失败: " + error.message);
        });
    }

    fetchScores();
})();
