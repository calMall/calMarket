"use client";

import React from "react";
import ContainImage from "../common/ContainImage";
import { newImageSizing } from "@/utils/newImageSizing";

interface props {
  item: CheckoutItem;
}
export default function CheckoutItem({ item }: props) {
  return (
    <div className="flex gap-1 cart-item-contain wf">
      <div className="rt cart-img">
        <ContainImage alt="product" url={newImageSizing(item.imageUrl, 256)} />
      </div>
      <div className="cart-item-info flex flex-col jb">
        <div className="cart-item-name">{item.itemName}</div>
        <div className="cart-quantity">
          <div className="flex ac jb ">数量：{item.quantity}</div>
          <div className="flex ae">
            ￥{(item.price * item.quantity).toLocaleString()}
          </div>
        </div>
      </div>
    </div>
  );
}
