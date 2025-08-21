"use client";

import { useEffect, useState } from "react";
import CartItem from "./CartItem";
import CustomButton from "../common/CustomBtn";
import { deleteCart, getCart } from "@/api/Cart";
import { useRouter } from "next/navigation";

interface props {
  initCartList: cartItem[];
}
export default function CartContain({ initCartList }: props) {
  const router = useRouter();
  const [cartList, setCartList] = useState(initCartList);
  const [checkList, setCheckList] = useState(initCartList);
  const reducePrice = () => {
    return checkList.reduce((acc, item) => acc + item.price * item.quantity, 0);
  };
  const [allPrice, setAllPrice] = useState(reducePrice());
  const refetchCart = async (type: "delete" | "change") => {
    try {
      const data = await getCart();
      setCartList(data.cartItems);
      if (type === "delete") setCheckList([]);
    } catch (e) {
      console.error("カートの再取得に失敗しました。", e);
    }
  };
  const onDelete = async () => {
    if (checkList.length === 0) {
      alert("商品が選択されていません。");
      return;
    }
    if (window.confirm("選択した商品を削除しますか？")) {
      try {
        const data = await deleteCart(checkList.map((item) => item.id));
        if (data.message === "success") {
          refetchCart("delete");
          alert("選択した商品を削除しました。");
        }
        // 削除処理をここに追加
      } catch (e) {}
    }
  };

  const checkItem = (item: cartItem) => {
    const isChecked = checkList.find((el) => el.id === item.id);

    if (isChecked) {
      setCheckList((prev) => prev.filter((el) => el.id !== item.id));
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
  const goCheckout = () => {
    if (checkList.length === 0) {
      alert("商品が選択されていません。");
      return;
    }
    const checkedIds = checkList.map((item) => item.id).join(",");
    router.push(`/order/checkout?ids=${checkedIds}`);
  };
  useEffect(() => {
    setAllPrice(reducePrice());
  }, [checkList]);

  return (
    <div>
      <h2>カート({cartList.length})</h2>
      <div className="bb wf jb flex pb-1">
        <div>
          <input
            type="checkbox"
            checked={checkList.length === cartList.length}
            onChange={checkAll}
          />
          すべて選択
        </div>
        <button className="cart-del-btn" onClick={onDelete}>
          選択した商品を削除
        </button>
      </div>
      <div className="cart-grid">
        <div className="mt-1 flex flex-col gap-1">
          {cartList.map((cart) => (
            <div className="flex ac wf rt" key={cart.id}>
              <input
                type="checkbox"
                checked={checkList.some((item) => item.id === cart.id)}
                onChange={() => checkItem(cart)}
              />
              <CartItem
                setCartList={setCartList}
                setCheckList={setCheckList}
                refetchData={refetchCart}
                initItem={cart}
              />
            </div>
          ))}
        </div>
        <div>
          <div className="cart-border mt-1">
            <div className="flex jb ac pd-1">
              <div>商品合計</div>
              <div>￥{allPrice.toLocaleString()}</div>
            </div>
            <div className="flex jb ac pd-1">
              <div>送料</div>
              <div>無料</div>
            </div>
            <div className="flex jb ac pd-1">
              <div>合計金額</div>
              <div>￥{allPrice.toLocaleString()}</div>
            </div>
          </div>
          <CustomButton
            classname="mt-1"
            text="お払いへ進む"
            func={goCheckout}
          />
        </div>
      </div>
    </div>
  );
}
