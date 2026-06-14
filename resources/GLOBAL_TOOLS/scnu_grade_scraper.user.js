// ==UserScript==
// @name         华师（正方）成绩抓取调试工具
// @namespace    http://tampermonkey.net/
// @version      1.2
// @description  在华南师范大学正方教务系统成绩页面添加"抓取成绩"按钮，以表格展示抓取结果
// @author       You
// @match        https://jwxt.scnu.edu.cn/cjcx/cjcx_cxDgXscj.html*
// @icon         https://www.scnu.edu.cn/favicon.ico
// @grant        none
// ==/UserScript==

(function () {
    "use strict";

    var REQUEST_URL = "/cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005";

    /**
     * 严格按浏览器"查询"按钮发送的参数格式构建请求
     */
    function getQueryParams() {
        var form = document.getElementById("searchForm");
        if (!form) throw new Error("未找到 searchForm");

        var params = new URLSearchParams();

        // 1. 序列化表单字段（跳过 Chosen 产生的 autocomplete）
        var elements = form.elements;
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            if (el.name && el.name !== "" && el.name !== "autocomplete" && el.type !== "submit") {
                params.append(el.name, el.value);
            }
        }

        // 2. 浏览器"查询"按钮发送的额外固定参数
        params.set("sfzgcj", "");
        params.set("kcbj", "");
        params.set("_search", "false");
        params.set("nd", String(Date.now()));
        params.set("queryModel.showCount", "15");
        params.set("queryModel.currentPage", "1");
        params.set("queryModel.sortName", " ");
        params.set("queryModel.sortOrder", "asc");
        params.set("time", "0");

        return params;
    }

    /**
     * 发送请求获取全部成绩数据
     */
    function fetchScores() {
        var params = getQueryParams();
        console.log("[抓取] 请求参数:", params.toString());

        return fetch(REQUEST_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest"
            },
            body: params.toString(),
            credentials: "include"
        }).then(function (r) {
            if (!r.ok) throw new Error("HTTP " + r.status);
            return r.json();
        }).then(function (data) {
            var rawItems = data.items && Array.isArray(data.items) ? data.items
                : data.rows && Array.isArray(data.rows) ? data.rows
                : Array.isArray(data) ? data
                : [];
            console.log("[抓取] API 返回 items 数:", rawItems.length, "totalResult:", data.totalResult);
            return rawItems.map(extractItem);
        });
    }

    function extractItem(raw) {
        if (raw.cell && Array.isArray(raw.cell)) {
            return {
                courseName: raw.cell[0] ? String(raw.cell[0]).trim() : "",
                credit: raw.cell[1] ? String(raw.cell[1]).trim() : "",
                score: raw.cell[2] ? String(raw.cell[2]).trim() : "",
                gpa: raw.cell[3] ? String(raw.cell[3]).trim() : ""
            };
        }
        return {
            courseName: (raw.kcmc || raw.courseName || "").trim(),
            credit: (raw.xf || raw.credit || "").toString().trim(),
            score: (raw.cj || raw.score || "").toString().trim(),
            gpa: (raw.jd || raw.gpa || "").toString().trim()
        };
    }

    /* ---------- 渲染表格 ---------- */

    function renderTable(tbody, data) {
        tbody.innerHTML = "";
        if (data.length === 0) {
            tbody.innerHTML = "<tr><td colspan='5' style='text-align:center;color:#999;'>无数据</td></tr>";
            return;
        }
        for (var i = 0; i < data.length; i++) {
            var tr = document.createElement("tr");
            tr.innerHTML =
                "<td style='padding:4px 8px;border-bottom:1px solid #eee;'>" + (i + 1) + "</td>" +
                "<td style='padding:4px 8px;border-bottom:1px solid #eee;'>" + esc(data[i].courseName) + "</td>" +
                "<td style='padding:4px 8px;border-bottom:1px solid #eee;'>" + esc(data[i].credit) + "</td>" +
                "<td style='padding:4px 8px;border-bottom:1px solid #eee;'>" + esc(data[i].score) + "</td>" +
                "<td style='padding:4px 8px;border-bottom:1px solid #eee;'>" + esc(data[i].gpa) + "</td>";
            tbody.appendChild(tr);
        }
    }

    function esc(str) {
        return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }

    /* ---------- 界面注入 ---------- */

    function injectUI() {
        // 结果面板
        var panel = document.createElement("div");
        panel.id = "gm_panel";
        panel.style.cssText =
            "position:fixed;top:60px;right:20px;width:750px;max-height:80vh;" +
            "background:#fff;border:1px solid #ccc;border-radius:8px;" +
            "box-shadow:0 4px 20px rgba(0,0,0,0.3);z-index:9999;" +
            "display:flex;flex-direction:column;font:14px/1.5 'Microsoft YaHei',sans-serif;";

        panel.innerHTML =
            '<div style="padding:10px 15px;background:#0770cd;color:#fff;' +
            'border-radius:8px 8px 0 0;display:flex;justify-content:space-between;align-items:center;">' +
            '<span style="font-weight:bold;">成绩抓取结果 (<span id="gm_count">0</span> 条)</span>' +
            '<span><button id="gm_copy" style="background:rgba(255,255,255,0.2);border:1px solid rgba(255,255,255,0.5);' +
            'color:#fff;border-radius:4px;padding:2px 10px;cursor:pointer;margin-right:8px;">复制 JSON</button>' +
            '<button id="gm_close" style="background:none;border:none;color:#fff;font-size:20px;cursor:pointer;">×</button></span>' +
            '</div>' +
            '<div style="padding:8px 15px;background:#f5f5f5;border-bottom:1px solid #eee;font-size:13px;color:#666;" id="gm_status">就绪</div>' +
            '<div style="overflow:auto;flex:1;">' +
            '<table style="width:100%;border-collapse:collapse;">' +
            '<thead><tr style="background:#f0f0f0;">' +
            '<th style="padding:6px 10px;text-align:left;border-bottom:1px solid #ddd;">#</th>' +
            '<th style="padding:6px 10px;text-align:left;border-bottom:1px solid #ddd;">课程名称</th>' +
            '<th style="padding:6px 10px;text-align:left;border-bottom:1px solid #ddd;">学分</th>' +
            '<th style="padding:6px 10px;text-align:left;border-bottom:1px solid #ddd;">成绩</th>' +
            '<th style="padding:6px 10px;text-align:left;border-bottom:1px solid #ddd;">绩点</th>' +
            '</tr></thead>' +
            '<tbody id="gm_body"></tbody>' +
            '</table></div>';

        document.body.appendChild(panel);

        // 关闭
        document.getElementById("gm_close").onclick = function () {
            panel.style.display = panel.style.display === "none" ? "" : "none";
        };

        // 复制 JSON
        document.getElementById("gm_copy").onclick = function () {
            var body = document.getElementById("gm_body");
            var rows = body.querySelectorAll("tr");
            var data = [];
            for (var i = 1; i < rows.length; i++) { // 跳过表头
                var cells = rows[i].querySelectorAll("td");
                if (cells.length >= 5) {
                    data.push({
                        courseName: cells[1].textContent,
                        credit: cells[2].textContent,
                        score: cells[3].textContent,
                        gpa: cells[4].textContent
                    });
                }
            }
            navigator.clipboard.writeText(JSON.stringify(data, null, 2)).then(function () {
                alert("已复制 " + data.length + " 条 JSON 到剪贴板");
            });
        };

        // 按钮
        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "btn btn-primary btn-sm";
        btn.style.cssText = "margin-left:10px;";
        btn.textContent = "抓取成绩";
        btn.onclick = function () {
            panel.style.display = "";
            btn.disabled = true;
            btn.textContent = "抓取中...";

            var statusEl = document.getElementById("gm_status");
            var tableBody = document.getElementById("gm_body");
            var countEl = document.getElementById("gm_count");
            statusEl.textContent = "请求中...";

            fetchScores().then(function (results) {
                countEl.textContent = results.length;
                renderTable(tableBody, results);
                statusEl.textContent = "抓取完成！共 " + results.length + " 条";
            }).catch(function (err) {
                statusEl.textContent = "失败: " + err.message;
            }).finally(function () {
                btn.disabled = false;
                btn.textContent = "抓取成绩";
            });
        };

        var searchBtn = document.getElementById("search_go");
        if (searchBtn && searchBtn.parentNode) {
            searchBtn.parentNode.appendChild(btn);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", injectUI);
    } else {
        injectUI();
    }
})();
