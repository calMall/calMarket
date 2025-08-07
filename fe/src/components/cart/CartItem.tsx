"use client";

import { useEffect, useState } from "react";
import ContainImage from "../common/ContainImage";
import CustomButton from "../common/CustomBtn";
import { deleteCart } from "@/api/Cart";
import { newImageSizing } from "@/utils/newImageSizing";

interface props {
  initItem: cartItem;
}
export default function CartItem({ initItem }: props) {
  const [item, setItem] = useState(initItem);
  const [mounted, setMounted] = useState(false);

  const quantityChange = async (cal: "-" | "+") => {
    if (cal === "+") {
      // APIロジック
      // const ex = await ex()
      setItem((prev) => {
        if (prev.quantity < 30) return { ...prev, quantity: prev.quantity + 1 };
        alert("注文は30件以下可能です。");
        return prev;
      });
    }
    if (cal === "-") {
      if (item.quantity > 1) {
        setItem((prev) => {
          return { ...prev, quantity: prev.quantity - 1 };
        });
      }
      if (item.quantity === 1) {
        if (window.confirm("商品を削除しますか？")) {
          // APIロジック
          // const data = await deleteCart()
          setItem;
        }
      }
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
