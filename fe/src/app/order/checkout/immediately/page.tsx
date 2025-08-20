"use client";
import { getCheckout } from "@/api/Cart";
import { postOrderByProduct } from "@/api/Order";
import { getProductDetail } from "@/api/Product";
import { getMyInfo } from "@/api/User";
import CustomButton from "@/components/common/CustomBtn";
import CustomLayout from "@/components/common/CustomLayout";
import ModalCover from "@/components/common/ModalCover";
import CheckoutItem from "@/components/order/CheckoutItem";
import DeliveryAddressModal from "@/components/user/DeliveryAddressModal";
import UserStore from "@/store/user";
import { get } from "http";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
interface props {
  searchParams: Promise<{ itemCode: string; quantity: number }>;
}

export default function CheckoutOrderImmediately({ searchParams }: props) {
  const userStore = UserStore();
  const [checkoutItem, setCheckoutItem] = useState<CheckoutItem>();
  const [isLoading, setIsLoading] = useState(true);
  const [allPrice, setAllPrice] = useState(0);
  const [myInfo, setMyInfo] = useState<MyinfoDTO | null>(null);
  const [selectedAddress, setSelectedAddress] =
    useState<deliveryAddressDetail | null>(null);
  const router = useRouter();
  const [quantity, setQuantity] = useState(1);
  // modalを表示するかどうか
  const [viewModal, setViewModal] = useState(false);
  const [itemCode, setItemCode] = useState("");

  function toCheckoutItem(temp: TempOrderItem, quantity: number): CheckoutItem {
    return {
      id: 0,
      imageUrl: temp.imageUrls[0] ?? "",
      itemCode: temp.itemCode,
      itemName: temp.itemName,
      price: temp.price,
      quantity: quantity,
    };
  }

  // カート情報ロード関数
  const fetchData = async () => {
    const { itemCode, quantity } = await searchParams;
    setItemCode(itemCode);
    setQuantity(quantity);
    try {
      const data = await getProductDetail(itemCode);
      console.log(data);
      if (data.message === "success") {
        const productWithQuantity = toCheckoutItem(data.product, quantity);
        setCheckoutItem(productWithQuantity);
        setAllPrice(productWithQuantity.price * quantity);
      }
      setIsLoading(false);
    } catch (e: any) {
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        return router.push("/login");
      }
      alert("カートの情報を取得できませんでした。");
      router.replace("/cart");
    }
  };

  // お届け先をロードする関数
  const onMyInfoChange = async () => {
    const data = await getMyInfo();
    if (data.message === "success") {
      console.log(data);
      setMyInfo(data);
      if (data.deliveryAddressDetails.length > 0) {
        setSelectedAddress(data.deliveryAddressDetails[0]);
      }
    }
  };

  const goCheckout = async () => {
    if (!selectedAddress) {
      return alert("お届け先住所を選択してください。");
    }
    try {
      const order: OrderRequestDto = {
        deliveryAddress: selectedAddress.postalCode + selectedAddress.address1,
        items: [
          {
            itemCode,
            quantity,
          },
        ],
      };
      const data = await postOrderByProduct(order);
      if (data.message === "success") {
        alert("注文が完了しました。");
        router.push("/");
      }
    } catch (e: any) {
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        return router.push("/login");
      }
      alert("カートの情報を取得できませんでした。");
      router.replace("/cart");
    }
  };

  useEffect(() => {
    fetchData();
    onMyInfoChange();
  }, []);

  return (
    <CustomLayout>
      {viewModal && (
        <ModalCover>
          <DeliveryAddressModal
            setMyInfo={setMyInfo}
            setSelectedAddress={setSelectedAddress}
            selectedAddress={selectedAddress}
            setViewModal={setViewModal}
            initialDeliveryAddress={myInfo?.deliveryAddressDetails}
          />
        </ModalCover>
      )}
      <div>
        <h2>ご注文内容の確認</h2>
        <div className="bb wf jb flex pb-1"></div>
        <div className="cart-grid">
          <div className="mt-1 flex flex-col gap-1">
            {isLoading ? (
              <p>Loading...</p>
            ) : checkoutItem ? (
              <>
                <CheckoutItem item={checkoutItem} />
              </>
            ) : (
              <></>
            )}
          </div>
          <div>
            <div className="cart-border mt-1">
              <div className="flex jb ac pd-1 ">
                <div>お届け先</div>
                <CustomButton
                  func={setViewModal.bind(null, true)}
                  text="変更"
                />
              </div>
              <div className="flex-col flex gap-1 pd-1">
                <div className="bb" />
                <div className="fw-500">{selectedAddress?.postalCode}</div>
                <div>{selectedAddress?.address1}</div>
              </div>
            </div>
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
                <div className="fw-600">￥{allPrice.toLocaleString()}</div>
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
    </CustomLayout>
  );
}
