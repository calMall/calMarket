"use client";
import { getCheckout } from "@/api/Cart";
import CustomLayout from "@/components/common/CustomLayout";
import CheckoutItem from "@/components/order/CheckoutItem";
import { useEffect, useState } from "react";
interface props {
  searchParams: Promise<{ ids: string }>;
}

export default function CheckoutOrder({ searchParams }: props) {
  const [checkoutList, setCheckoutList] = useState<CheckoutItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [allPrice, setAllPrice] = useState(0);
  const fetchData = async () => {
    const { ids } = await searchParams;
    const data = await getCheckout(ids.split(",").map((id) => Number(id)));
    setCheckoutList(data.cartList);
    console.log(data.cartList);
    // setAllPrice(data.cartList.reduce((acc, item) => acc + item * item.quantity, 0));
    setIsLoading(false);
  };
  useEffect(() => {
    fetchData();
  }, []);

  return (
    <CustomLayout>
      <div>
        <h2>ご注文内容の確認</h2>
        <div className="bb wf jb flex pb-1"></div>
        <div className="cart-grid">
          <div className="mt-1 flex flex-col gap-1">
            {isLoading ? (
              <p>Loading...</p>
            ) : (
              <>
                {checkoutList.map((item) => (
                  <CheckoutItem key={item.id} item={item} />
                ))}
              </>
            )}
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
            {/* <CustomButton
              classname="mt-1"
              text="お払いへ進む"
              func={goCheckout}
            /> */}
          </div>
        </div>
      </div>
    </CustomLayout>
  );
}
