"use client";

import React from "react";
import ContainImage from "../common/ContainImage";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";

interface props {
  item: OrderInfoOnList;
}
export default function OrderedItem({ item }: props) {
  return (
    <Link
      href={`/order/list/detail/${item.orderId}`}
      className="flex gap-1 order-list-item-contain wf bb"
    >
      <div className="hf flex ac">
        <div className="rt cart-img">
          <ContainImage
            alt="product"
            url={newImageSizing(item.imageList[0], 256)}
          />
        </div>
      </div>
      <div className="cart-item-info flex flex-col jb">
        <div className="cart-item-name">{item.itemName}</div>
        <div className="cart-quantity">
          <div className="flex ac ">数量：{item.quantity}</div>
        </div>
        <div className="wf flex order-price-contain">
          <h4>注文詳細を確認する</h4>
          <h4>￥{(item.price * item.quantity).toLocaleString()}</h4>
        </div>
      </div>
    </Link>
  );
}
