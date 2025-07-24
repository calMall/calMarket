"use client";
import { ChangeEvent, useEffect, useState } from "react";
import CustomButton from "../common/CustomBtn";
import { FiShoppingCart } from "react-icons/fi";
import Link from "next/link";
import { RiShoppingBag4Line } from "react-icons/ri";
import { LuShare } from "react-icons/lu";

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

  const onCart = () => {};

  function copy() {
    navigator.clipboard.writeText(url);

    alert("コピーされました。");
  }

  useEffect(() => {
    setUrl(window.location.href);
  }, []);
  useEffect(() => {
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
              if (prev > 0) return prev - 1;
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
