"use client";

import { myInfo } from "@/api/User";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import Star from "@/components/product/Star";
import UserStore from "@/store/user";
import Link from "next/link";
import { useEffect, useState } from "react";

export default function Mypage() {
  const userStore = UserStore();
  const [point, setPoint] = useState<null | number>(null);
  const [orders, setOrders] = useState<SimpleOrder[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  useEffect(() => {
    const setData = async () => {
      try {
        const data = await myInfo();
        setPoint(data.point);
        setOrders(data.orders);
        setReviews(data.reviews);
      } catch (e) {
        console.log(e);
        alert("エラーが発生しました。");
      }
    };
    setData();
  }, []);
  return (
    <CustomLayout>
      <div className="bb mypage-horizens">
        <h2>ポイント残高</h2>
        {point}ポイント
      </div>
      <div className="bb mypage-horizens">
        <h2>注文履歴</h2>
        {orders.map((order) => (
          <div>{order.imageUrl}</div>
        ))}
        <div className="bb mypage-horizens">
          <h2>注文履歴</h2>
          {orders.length === 0 ? (
            <div>注文履歴がありません</div>
          ) : (
            orders.map((order) => <div key={order.id}>{order.imageUrl}</div>)
          )}
        </div>
        {/* オーダーできたら上に移す */}
        <div className="wf simple-order-contain">
          <Link href={`/`} className="rt simple-order-img">
            <ContainImage alt="product" url="/a.png" />
          </Link>
        </div>
        {/*  */}
      </div>
      <div>
        <div className="mypage-horizens">
          <h2>「{userStore.userInfo?.nickname}」さんのレビュー</h2>
          {reviews.length === 0 ? (
            <div>レビューがありません</div>
          ) : (
            <>
              {reviews.map((review) => (
                <div key={review.id}>
                  <div>{review.title}</div>
                  <Star score={review.score} />
                  <div>{review.createdAt}</div>
                  <div>{review.content}</div>
                </div>
              ))}
            </>
          )}
        </div>

        {/* レビューできたら上に移す */}
        <div className="wf simple-order-contain">
          <Link href={`/`} className="simple-order-img pd-1">
            <div className="review-title ">
              asdasdasdasdasdasdasdasdasdasdasdasdasd
            </div>
            <Star score={4} />
            <div className="mt-05">2025-10-10</div>
            <div className="review-content mt-05">
              sdfsadsfasdkjfhajkehfouiawefhkjsbfjksofafoheuiaowefhiauehfiuasebfkjsadbfjkasdbflasdfjadksfsajkdhfaksjdh
            </div>
          </Link>
        </div>
        {/*  */}
      </div>
    </CustomLayout>
  );
}
