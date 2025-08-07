"use client";
import { ChangeEvent, useEffect, useState } from "react";
import CustomButton from "../common/CustomBtn";
import { FiShoppingCart } from "react-icons/fi";
import Link from "next/link";
import { RiShoppingBag4Line } from "react-icons/ri";
import { LuShare } from "react-icons/lu";
import { postCart } from "@/api/Cart";

interface props {
  itemCode: string;
}
export default function ProductDetailCartBtns({ itemCode }: props) {
  const [quantity, setQuantity] = useState(1);
  const [url, setUrl] = useState("");
  const changeQuantity = (e: ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    if (raw === "") {
      setQuantity(1);
      return;
    }

    if (!/^\d+$/.test(raw)) return;
    const num = Number(raw);
    setQuantity(num);
  };

  const onCart = async () => {
    if (quantity < 1) {
      return alert("数量は1以上でなければなりません。");
    }
    if (quantity > 30) {
      return alert("注文は30件以下可能です。");
    }
    try {
      const res = await postCart(itemCode, quantity);
      console.log(res);

      if (res.message === "success") {
        alert("カートに商品が追加されました。");
      }
    } catch (error) {
      console.error(error);
      alert("カートに商品を追加する際にエラーが発生しました。");
    }
  };

  function copy() {
    navigator.clipboard.writeText(url);

    alert("コピーされました。");
  }

  useEffect(() => {
    setUrl(window.location.href);
  }, []);
  useEffect(() => {
    if (quantity < 1) {
      return setQuantity(1);
    }
    if (quantity > 30) {
      alert("注文は30件以下可能です。");
      return setQuantity(30);
    }
  }, [quantity]);

  return (
    <div className="mt-2">
      <div className="flex ac jb cart-quantity-contain">
        <CustomButton
          classname="cart-quantity-btn fw-500"
          text="-"
          func={() =>
            setQuantity((prev) => {
              if (prev > 1) return prev - 1;
              return prev;
            })
          }
        />
        <input
          className="cart-quantity-input"
          type="text"
          value={quantity}
          onChange={changeQuantity}
        />
        <CustomButton
          classname="cart-quantity-btn fw-500"
          text="+"
          func={() => setQuantity((prev) => prev + 1)}
        />
      </div>
      <button
        onClick={onCart}
        className="on-cart-btn mt-1 gap-05 flex jc ac custom-btn "
      >
        <FiShoppingCart />
        カートに入れる
      </button>
      <div className="flex jb mt-1">
        <Link href={`/`} className="product-detail-white-btn">
          <RiShoppingBag4Line />
          今すぐ買う
        </Link>
        <button onClick={copy} className="product-detail-white-btn">
          <LuShare />
          シェアする
        </button>
      </div>
    </div>
  );
}
