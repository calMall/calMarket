"use client";

import { myInfo } from "@/api/User";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import Star from "@/components/product/Star";
import UserStore from "@/store/user";
import { dateFormat } from "@/utils/dateFormat";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function Mypage() {
  const userStore = UserStore();
  const router = useRouter();
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
        console.log(data);
      } catch (e: any) {
        console.log(e);
        if (e.status === 401) {
          alert("ログインが必要です。ログインページに移動します。");
          userStore.logout();
          router.push("/login");
        } else {
          alert("レビューの投稿中にエラーが発生しました。");
        }
      }
    };
    setData();
  }, []);
  return (
    <CustomLayout>
      <div className="bb mypage-horizens">
        <h2>ポイント残高</h2>
        {point?.toLocaleString()} ポイント
      </div>
      <div className="bb mypage-horizens">
        <div className="flex jb">
          <h2>注文履歴</h2>
          <Link
            className="flex ac color-deep-dark-main"
            href={"/mypage/orders"}
          >
            もっと見る
          </Link>
        </div>
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
          {reviews.slice(0, 4).map((review) => (
            <Link
              key={review.id}
              href={`/review/detail/${review.id}`}
              className="simple-order-img pd-1"
            >
              <div className="review-title">{review.title}</div>
              <Star score={review.score} />
              <div className="mt-05 review-date">
                {dateFormat(review.createdAt)}
              </div>
              <div className="review-content mt-05">{review.content} </div>
            </Link>
          ))}
        </div>
      </div>
    </CustomLayout>
  );
}
