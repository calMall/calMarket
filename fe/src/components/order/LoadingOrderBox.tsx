export default function LoadingOrderBox() {
  return (
    <div className="order-item bb mb-1 pb-1">
      <div className="flex gap-1">
        <div className="loading-box order-loading" />
        <div className="flex flex-col gap-05 wf">
          <div className="loading-box order-loading-text" />
          <div className="loading-box order-loading-text" />
        </div>
      </div>
    </div>
  );
}
