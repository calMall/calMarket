"use client";

export default function OderListLoadingBox() {
  return (
    <div className="flex gap-1 order-list-item-contain wf bb mt-1">
      <div className="hf flex ac">
        <div className="rt cart-img loading-box " />
      </div>
      <div className="flex flex-col gap-05 wf">
        <div className="order-loding-text loading-box " />
        <div className="order-loding-text-50 loading-box " />
        <div className="order-loding-text-20 loading-box mt-2" />
        <div className="order-loding-text-20 loading-box  mt-1" />
      </div>
    </div>
  );
}
