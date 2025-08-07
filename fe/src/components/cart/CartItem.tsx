"use client";

import React, { useEffect, useState } from "react";
import ContainImage from "../common/ContainImage";
import CustomButton from "../common/CustomBtn";
import { decreaseProduct, deleteCart, increaseProduct } from "@/api/Cart";
import { newImageSizing } from "@/utils/newImageSizing";

interface props {
  initItem: cartItem;
  refetchData: Function;
  setCheckList: React.Dispatch<React.SetStateAction<cartItem[]>>;
}
export default function CartItem({
  initItem,
  refetchData,
  setCheckList,
}: props) {
  const [item, setItem] = useState(initItem);
  const [mounted, setMounted] = useState(false);

  const quantityChange = async (cal: "-" | "+") => {
    if (cal === "+") {
      if (item.quantity < 30) {
        try {
          const res = await increaseProduct(item.id);
          console.log(res);
          if (res.message === "success") {
            await refetchData();
            setCheckList((prev) => {
              return prev.map((el) =>
                el.id === item.id ? { ...el, quantity: el.quantity + 1 } : el
              );
            });
            return setItem((prev) => {
              return { ...prev, quantity: prev.quantity + 1 };
            });
          }
        } catch (e) {
          return alert("追加に失敗しました。");
        }
      }
      return alert("注文は30件以下可能です。");
    }

    if (cal === "-") {
      if (item.quantity > 1) {
        try {
          const res = await decreaseProduct(item.id);
          console.log(res);
          if (res.message === "success") {
            await refetchData();
            setCheckList((prev) => {
              return prev.map((el) =>
                el.id === item.id ? { ...el, quantity: el.quantity - 1 } : el
              );
            });
            return setItem((prev) => {
              return { ...prev, quantity: prev.quantity - 1 };
            });
          }
        } catch (e) {
          return alert("削除に失敗しました。");
        }
      }
      return alert("注文は1件以下可能です。");
    }
  };

  useEffect(() => {
    setMounted(true);
  }, []);
  return (
    <div className="flex gap-1 cart-item-contain wf">
      <div className="rt cart-img">
        <ContainImage
          alt="product"
          url={newImageSizing(item.imageUrls[0], 256)}
        />
      </div>
      <div className="cart-item-info flex flex-col jb">
        <div className="cart-item-name">{item.itemName}</div>
        <div className="cart-quantity">
          <div className="flex ac jb cart-quantity-contain">
            <CustomButton
              classname="cart-quantity-btn fw-500"
              text="-"
              func={quantityChange.bind(mounted ? window : null, "-")}
            />
            <div className="cart-quantity-input flex ac jc">
              {item.quantity}
            </div>
            <CustomButton
              classname="cart-quantity-btn fw-500"
              text="+"
              func={quantityChange.bind(mounted ? window : null, "+")}
            />
          </div>

          <div className="flex ae">
            ￥{(item.price * item.quantity).toLocaleString()}
          </div>
        </div>
      </div>
    </div>
  );
}
