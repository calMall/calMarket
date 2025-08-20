import CustomLayout from "./CustomLayout";

export default function ErrorComponent() {
  return (
    <CustomLayout>
      <div className="error-component">
        <h1>エラーが発生しました</h1>
        <p>申し訳ありませんが、ページを表示できませんでした。</p>
      </div>
    </CustomLayout>
  );
}
