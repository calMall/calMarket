"use client";

import { useState } from "react";
import CartItem from "./CartItem";
import CustomButton from "../common/CustomBtn";

interface props {
  initCartList: cartItem[];
}
export default function CartContain({ initCartList }: props) {
  const [cartList, setCartList] = useState(initCartList);
  const [checkList, setCheckList] = useState(initCartList);

  const checkItem = (item: cartItem) => {
    const isChecked = checkList.find((el) => el.itemCode === item.itemCode);

    if (isChecked) {
      setCheckList((prev) =>
        prev.filter((el) => el.itemCode !== item.itemCode)
      );
    } else {
      setCheckList((prev) => [...prev, item]);
    }
  };
  const checkAll = () => {
    if (checkList.length === cartList.length) {
      setCheckList([]);
    } else {
      setCheckList(cartList);
    }
  };
  return (
    <div>
      <h2>カート({initCartList.length})</h2>
      <div className="bb wf jb flex pb-1">
        <div>
          <input
            type="checkbox"
            value={""}
            defaultChecked={checkList.length === cartList.length}
            onClick={checkAll}
          />
          すべて選択
        </div>
        <button>選択した商品を削除</button>
      </div>
      <div className="cart-grid">
        <div className="mt-1 flex flex-col gap-1">
          {cartList.map((cart) => (
            <CartItem initItem={cart} key={cart.itemCode} />
          ))}
        </div>
        <div>
          <div className="cart-border mt-1">
            <div className="flex jb ac pd-1">
              <div>商品合計</div>
              <div>
                ￥
                {checkList
                  .reduce((acc, item) => acc + item.price * item.quantity, 0)
                  .toLocaleString()}
              </div>
            </div>
            <div className="flex jb ac pd-1">
              <div>送料</div>
              <div>無料</div>
            </div>
            <div className="flex jb ac pd-1">
              <div>合計金額</div>
              <div>
                ￥
                {checkList
                  .reduce((acc, item) => acc + item.price * item.quantity, 0)
                  .toLocaleString()}
              </div>
            </div>
          </div>
          <CustomButton
            classname="mt-1"
            text="お払いへ進む"
            func={() => console.log("購入手続き")}
          />
        </div>
      </div>
    </div>
  );
}
