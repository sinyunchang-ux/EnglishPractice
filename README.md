# 英文句子練習 App

## 如何在 Android Studio 開啟並執行

### 第一次開啟
1. **複製專案資料夾**到你想放的位置（不要直接用這個資料夾，Android Studio 需要乾淨的資料夾）
2. **Android Studio → File → Open**
3. 選你複製過去的那個資料夾，按 OK
4. 第一次開啟時，Android Studio 會問你要不要下載 Gradle，按 **Yes / 允許**
5. 等它下載完成（視網速約 2~5 分鐘），就會自動 Sync 完成
6. 按 ▶ (Run) 就會裝到手機上

### 若遇到問題

**問題：Gradle 下載太慢或失敗**
- 打開 `gradle/wrapper/gradle-wrapper.properties`
- 把第三行改成：
  ```
  distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip
  ```
- 然後 Android Studio → File → Sync Project with Gradle Files

**問題：Kotlin 版本不相容**
- 確保你的 Android Studio 是 2024 年之後的版本

## 功能說明

| 功能 | 操作位置 |
|------|----------|
| 新增筆記 | 試算表頁面 → 右下角 + |
| 匯入 CSV | 試算表頁面 → 右上角 📂 圖示 |
| 錄音 | 點進筆記 → 點中間麥克風按鈕 |
| 播放錄音 | 試算表頁面 ▶ 按鈕 |
| 分享到 LINE | 試算表頁面 ↗ 按鈕 |
| 月曆檢視 | 底部 Tab 切換到「月曆」|
| 編輯/刪除 | 試算表頁面點擊該列 |

## CSV 格式

```
english,chinese
Hello,你好
Good morning,早安
```

- 第一列會自動當 header 跳過（如果同時有 "english" 和 "chinese"）
- 匯入時用「英文內容」去重，相同的會跳過