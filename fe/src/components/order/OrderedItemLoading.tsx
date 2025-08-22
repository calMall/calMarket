interface props {
  item: OrderInfoOnList;
}
export default function OrderedItemLoading() {
  return (
    <div className="flex gap-1 cart-item-contain wf ">
      <div className="rt cart-img loading-box" />
      <div className="wf flex flex-col gap-05">
        <div className="order-loading-text loading-box mt-1"></div>
        <div className="order-loading-text loading-box"></div>
        <div className="order-loading-text-20 loading-box mt-2"></div>
        <div className="order-loading-text-20 loading-box"></div>
      </div>
    </div>
  );
}
