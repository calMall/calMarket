"use client";

import React, { useEffect, useState } from "react";
import ContainImage from "../common/ContainImage";
import CustomButton from "../common/CustomBtn";
import { decreaseProduct, deleteCart, increaseProduct } from "@/api/Cart";
import { newImageSizing } from "@/utils/newImageSizing";
import { MdDeleteOutline } from "react-icons/md";

interface props {
  initItem: cartItem;
  refetchData: (type: "delete" | "change", item?: cartItem) => void;
  setCheckList: React.Dispatch<React.SetStateAction<cartItem[]>>;
  setCartList: React.Dispatch<React.SetStateAction<cartItem[]>>;
}
export default function CartItem({
  initItem,
  refetchData,
  setCheckList,
  setCartList,
}: props) {
  const [item, setItem] = useState(initItem);
  const [mounted, setMounted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const quantityChange = async (cal: "-" | "+") => {
    if (cal === "+") {
      if (item.quantity < 30) {
        try {
          setIsLoading(true);
          const res = await increaseProduct(item.id);
          setIsLoading(false);
          if (res.message === "success") {
            refetchData("change", { ...item, quantity: item.quantity + 1 });
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
          setIsLoading(false);
          return alert("追加に失敗しました。");
        }
      }
      return alert("注文は30件以下可能です。");
    }

    if (cal === "-") {
      if (item.quantity > 1) {
        try {
          setIsLoading(true);
          const res = await decreaseProduct(item.id);
          setIsLoading(false);
          if (res.message === "success") {
            refetchData("change", { ...item, quantity: item.quantity - 1 });
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
          setIsLoading(false);
          return alert("削除に失敗しました。");
        }
      }
      if (item.quantity === 1 && window.confirm("商品を削除しますか？")) {
        try {
          setIsLoading(true);
          const res = await deleteCart([item.id]);
          setIsLoading(false);
          if (res.message === "success") {
            refetchData("delete");
          }
        } catch (e) {
          setIsLoading(false);
          return alert("削除に失敗しました。");
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
              disable={isLoading}
              classname="cart-quantity-btn fw-500"
              text={item.quantity > 1 ? "-" : ""}
              icon={item.quantity <= 1 ? <MdDeleteOutline /> : null}
              func={quantityChange.bind(mounted ? window : null, "-")}
            />

            <div className="cart-quantity-input flex ac jc">
              {item.quantity}
            </div>
            <CustomButton
              disable={isLoading}
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
