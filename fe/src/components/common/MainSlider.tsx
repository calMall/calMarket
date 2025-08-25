"use client";
import "keen-slider/keen-slider.min.css";
import { useKeenSlider } from "keen-slider/react";
import { useState } from "react";
import CoverImage from "./CoverImage";

function Arrow(props: {
  disabled: boolean;
  left?: boolean;
  onClick: (e: any) => void;
}) {
  const disabled = props.disabled ? " arrow--disabled" : "";
  return (
    <button
      className={`arrow ${
        props.left ? "arrow--left" : "arrow--right"
      } ${disabled}`}
    >
      <svg
        onClick={props.onClick}
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
      >
        {props.left && (
          <path d="M16.67 0l2.83 2.829-9.339 9.175 9.339 9.167-2.83 2.829-12.17-11.996z" />
        )}
        {!props.left && (
          <path d="M5 3l3.057-3 11.943 12-11.943 12-3.057-3 9-9z" />
        )}
      </svg>
    </button>
  );
}

export default function MainSlider() {
  const [loaded, setLoaded] = useState(false);
  const [currentSlide, setCurrentSlide] = useState(0);
  const eventList = [
    "/event1.jpg",
    "/event2.jpg",
    "/event3.jpg",
    "/event4.jpg",
  ];
  const [sliderRef, instanceRef] = useKeenSlider(
    {
      loop: true,
      slideChanged(slider) {
        setCurrentSlide(slider.track.details.rel);
      },
      created() {
        setLoaded(true);
      },
    },
    [
      (slider) => {
        let timeout: NodeJS.Timeout;
        let mouseOver = false;
        function clearNextTimeout() {
          clearTimeout(timeout);
        }
        function nextTimeout() {
          clearTimeout(timeout);
          if (mouseOver) return;
          timeout = setTimeout(() => {
            slider.next();
          }, 2000);
        }
        slider.on("created", () => {
          slider.container.addEventListener("mouseover", () => {
            mouseOver = true;
            clearNextTimeout();
          });
          slider.container.addEventListener("mouseout", () => {
            mouseOver = false;
            nextTimeout();
          });
          nextTimeout();
        });
        slider.on("dragStarted", clearNextTimeout);
        slider.on("animationEnded", nextTimeout);
        slider.on("updated", nextTimeout);
      },
    ]
  );
  return (
    <div className="rt main-slider-contain">
      <div ref={sliderRef} className="keen-slider">
        {eventList.map((event) => (
          <div key={event} className="keen-slider__slide">
            <div className="main-slider ">
              <CoverImage url={event} alt="event" />
            </div>
          </div>
        ))}
      </div>
      {loaded && instanceRef.current && (
        <>
          <Arrow
            disabled={false}
            left
            onClick={(e: any) =>
              e.stopPropagation() || instanceRef.current?.prev()
            }
          />

          <Arrow
            disabled={false}
            onClick={(e: any) =>
              e.stopPropagation() || instanceRef.current?.next()
            }
          />
        </>
      )}
    </div>
  );
}
